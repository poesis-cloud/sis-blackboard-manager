package cloud.poesis.sis.bbman.controller;

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

/** Edge-case integration tests covering remaining service branches. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServiceEdgeCasesIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    private UUID createBlackboard(String name) throws Exception {
        MvcResult r = mvc.perform(
                post("/api/v1/blackboards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(om.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID declarePanel(UUID bbId, String panelName, String slotName, String schema)
            throws Exception {
        Map<String, Object> body = Map.of(
                "name", panelName,
                "description", "d",
                "slots", List.of(Map.of("name", slotName, "schemaUri", schema)));
        MvcResult r = mvc.perform(
                post("/api/v1/blackboards/{id}/panels", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(om.readTree(r.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void slotSchemaUnresolvable_returns400() throws Exception {
        UUID bbId = createBlackboard("bb-bad-schema");
        Map<String, Object> body = Map.of(
                "name", "p",
                "description", "d",
                "slots", List.of(Map.of("name", "s", "schemaUri", "classpath:does/not/exist.json")));
        mvc.perform(
                post("/api/v1/blackboards/{id}/panels", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("schemaUri")));
    }

    @Test
    void duplicateSlotInPanel_returns400() throws Exception {
        UUID bbId = createBlackboard("bb-dup-slot");
        Map<String, Object> body = Map.of(
                "name", "p",
                "description", "d",
                "slots",
                List.of(
                        Map.of("name", "s", "schemaUri", "classpath:schemas/passthrough.json"),
                        Map.of("name", "s", "schemaUri", "classpath:schemas/passthrough.json")));
        mvc.perform(
                post("/api/v1/blackboards/{id}/panels", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStateOfNewBlackboard_zeroContributions() throws Exception {
        UUID bbId = createBlackboard("bb-state-empty");
        mvc.perform(get("/api/v1/blackboards/{id}/state", bbId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contributionCount").value(0))
                .andExpect(jsonPath("$.panels").isArray());
    }

    @Test
    void listPanelsAndContributionsByFilter() throws Exception {
        UUID bbId = createBlackboard("bb-filter");
        declarePanel(bbId, "p1", "s1", "classpath:schemas/passthrough.json");

        ObjectNode payload = om.createObjectNode().put("k", "v");
        mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of("panel", "p1", "slot", "s1", "post", payload))))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/blackboards/{id}/panels", bbId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("p1"));

        mvc.perform(get("/api/v1/blackboards/{id}/contributions", bbId).param("panel", "p1").param("slot", "s1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slot").value("s1"));
    }

    @Test
    void contributionWithDerivedAcrossBlackboard_returns422() throws Exception {
        UUID bbA = createBlackboard("bb-A");
        declarePanel(bbA, "p", "s", "classpath:schemas/passthrough.json");
        ObjectNode pa = om.createObjectNode().put("a", 1);
        MvcResult rA = mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of("panel", "p", "slot", "s", "post", pa))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID cA = UUID.fromString(om.readTree(rA.getResponse().getContentAsString()).get("id").asText());

        UUID bbB = createBlackboard("bb-B");
        declarePanel(bbB, "p", "s", "classpath:schemas/passthrough.json");
        ObjectNode pb = om.createObjectNode().put("b", 2);
        mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of(
                                                "panel", "p",
                                                "slot", "s",
                                                "post", pb,
                                                "derivedContribution", List.of(cA.toString())))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void contributionWithUnknownDerived_returns422() throws Exception {
        UUID bbId = createBlackboard("bb-derived-unknown");
        declarePanel(bbId, "p", "s", "classpath:schemas/passthrough.json");
        mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of(
                                                "panel", "p",
                                                "slot", "s",
                                                "post", om.createObjectNode().put("k", 1),
                                                "derivedContribution", List.of(UUID.randomUUID().toString())))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void contributionWithKnownDerived_succeeds() throws Exception {
        UUID bbId = createBlackboard("bb-derived-ok");
        declarePanel(bbId, "p", "s", "classpath:schemas/passthrough.json");
        ObjectNode payload = om.createObjectNode().put("k", 1);
        MvcResult r = mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of("panel", "p", "slot", "s", "post", payload))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID cId = UUID.fromString(om.readTree(r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of(
                                                "panel", "p",
                                                "slot", "s",
                                                "post", payload,
                                                "derivedContribution", List.of(cId.toString())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.derivedContribution[0]").value(cId.toString()));
    }

    @Test
    void contributionOnSealedBlackboard_returns409() throws Exception {
        UUID bbId = createBlackboard("bb-sealed-write");
        declarePanel(bbId, "p", "s", "classpath:schemas/passthrough.json");
        mvc.perform(post("/api/v1/blackboards/{id}/seal", bbId)).andExpect(status().isOk());
        mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of("panel", "p", "slot", "s", "post", om.createObjectNode()))))
                .andExpect(status().isConflict());
    }

    @Test
    void blankPanelDescriptionRejected() throws Exception {
        UUID bbId = createBlackboard("bb-blank-desc");
        Map<String, Object> body = Map.of(
                "name", "p",
                "description", " ",
                "slots", List.of(Map.of("name", "s", "schemaUri", "classpath:schemas/passthrough.json")));
        mvc.perform(
                post("/api/v1/blackboards/{id}/panels", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contributionSchemaViolation_returns422() throws Exception {
        UUID bbId = createBlackboard("bb-schema-violation");
        declarePanel(bbId, "p", "s", "classpath:schemas/strict-object-required-x.json");
        mvc.perform(
                post("/api/v1/blackboards/{id}/contributions", bbId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                om.writeValueAsString(
                                        Map.of("panel", "p", "slot", "s", "post", om.createObjectNode()))))
                .andExpect(status().isUnprocessableEntity());
    }
}
