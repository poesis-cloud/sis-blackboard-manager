package cloud.poesis.sie.bbman.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlackboardControllerIntegrationTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;

  private UUID createBlackboard(String name) throws Exception {
    MvcResult r =
        mvc.perform(
                post("/api/v1/blackboards")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(Map.of("name", name))))
            .andExpect(status().isCreated())
            .andReturn();
    return UUID.fromString(om.readTree(r.getResponse().getContentAsString()).get("id").asText());
  }

  @Test
  void createBlackboard_returnsOpen() throws Exception {
    mvc.perform(
            post("/api/v1/blackboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("name", "bb-create"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("bb-create"))
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  @Test
  void createBlackboard_blankNameRejected() throws Exception {
    mvc.perform(
            post("/api/v1/blackboards")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("name", " "))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getBlackboard_notFound() throws Exception {
    mvc.perform(get("/api/v1/blackboards/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("not found")));
  }

  @Test
  void declarePanelAndPostContribution_andList() throws Exception {
    UUID bbId = createBlackboard("bb-flow");

    Map<String, Object> panel =
        Map.of(
            "name", "panel-1",
            "description", "first panel",
            "slots",
                List.of(
                    Map.of(
                        "name", "slot-1",
                        "schemaUri",
                            "classpath:" + JsonSchemaResource.PASSTHROUGH_SCHEMA)));

    mvc.perform(
            post("/api/v1/blackboards/{id}/panels", bbId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(panel)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("panel-1"))
        .andExpect(jsonPath("$.slots[0].name").value("slot-1"));

    ObjectNode payload = om.createObjectNode();
    payload.put("hello", "world");
    Map<String, Object> contrib =
        Map.of("panel", "panel-1", "slot", "slot-1", "post", payload);

    mvc.perform(
            post("/api/v1/blackboards/{id}/contributions", bbId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(contrib)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.panel").value("panel-1"))
        .andExpect(jsonPath("$.slot").value("slot-1"))
        .andExpect(jsonPath("$.post.hello").value("world"));

    mvc.perform(get("/api/v1/blackboards/{id}/contributions", bbId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].post.hello").value("world"));

    mvc.perform(get("/api/v1/blackboards/{id}/state", bbId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contributionCount").value(1))
        .andExpect(jsonPath("$.panels[0].name").value("panel-1"));

    // Adding a panel after first contribution must fail with 409 PANEL_TOPOLOGY_FROZEN.
    mvc.perform(
            post("/api/v1/blackboards/{id}/panels", bbId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of(
                    "name", "panel-2",
                    "description", "late",
                    "slots", List.of(Map.of("name", "x",
                        "schemaUri", "classpath:" + JsonSchemaResource.PASSTHROUGH_SCHEMA))))))
        .andExpect(status().isConflict());
  }

  @Test
  void seal_thenWriteRejected() throws Exception {
    UUID bbId = createBlackboard("bb-seal");

    mvc.perform(post("/api/v1/blackboards/{id}/seal", bbId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SEALED"))
        .andExpect(jsonPath("$.sealTimestamp").exists());

    // Cannot seal again.
    mvc.perform(post("/api/v1/blackboards/{id}/seal", bbId)).andExpect(status().isConflict());
    // Cannot abort terminal.
    mvc.perform(post("/api/v1/blackboards/{id}/abort", bbId)).andExpect(status().isConflict());
  }

  @Test
  void abort_thenSealRejected() throws Exception {
    UUID bbId = createBlackboard("bb-abort");

    mvc.perform(post("/api/v1/blackboards/{id}/abort", bbId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ABORTED"));

    mvc.perform(post("/api/v1/blackboards/{id}/seal", bbId)).andExpect(status().isConflict());
  }

  @Test
  void unknownPanelOnContribution_returns422() throws Exception {
    UUID bbId = createBlackboard("bb-422");
    ObjectNode post = om.createObjectNode();
    post.put("a", 1);
    Map<String, Object> contrib = Map.of("panel", "no", "slot", "no", "post", post);

    mvc.perform(
            post("/api/v1/blackboards/{id}/contributions", bbId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(contrib)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void reservedPanelPrefixRejected() throws Exception {
    UUID bbId = createBlackboard("bb-reserved");
    Map<String, Object> panel =
        Map.of(
            "name", "bb:reserved",
            "description", "x",
            "slots",
                List.of(Map.of("name", "s",
                    "schemaUri", "classpath:" + JsonSchemaResource.PASSTHROUGH_SCHEMA)));

    MvcResult r =
        mvc.perform(
                post("/api/v1/blackboards/{id}/panels", bbId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(panel)))
            .andExpect(status().isBadRequest())
            .andReturn();
    assertThat(r.getResponse().getContentAsString()).contains("reserved");
  }
}
