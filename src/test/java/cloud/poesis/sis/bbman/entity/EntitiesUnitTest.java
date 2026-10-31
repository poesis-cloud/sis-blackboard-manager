package cloud.poesis.sis.bbman.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntitiesUnitTest {

  @Test
  void blackboardConstructorAndAccessors() {
    BlackboardEntity bb = new BlackboardEntity("name");
    assertThat(bb.getName()).isEqualTo("name");
    assertThat(bb.getStatus()).isNotNull();
    assertThat(bb.getId()).isNull();
    assertThat(bb.getTimestamp()).isNull();
    assertThat(bb.getSealTimestamp()).isNull();
    assertThat(bb.getPanels()).isEmpty();
    bb.setSealTimestamp(java.time.Instant.EPOCH);
    assertThat(bb.getSealTimestamp()).isEqualTo(java.time.Instant.EPOCH);
    bb.setStatus(cloud.poesis.sis.bbman.type.BlackboardStatus.SEALING);
    assertThat(bb.getStatus()).isEqualTo(cloud.poesis.sis.bbman.type.BlackboardStatus.SEALING);
  }

  @Test
  void panelConstructorAndAccessors() {
    BlackboardEntity bb = new BlackboardEntity("bb");
    PanelEntity p = new PanelEntity(bb, "p", "desc");
    assertThat(p.getBlackboard()).isSameAs(bb);
    assertThat(p.getName()).isEqualTo("p");
    assertThat(p.getDescription()).isEqualTo("desc");
    assertThat(p.getSlots()).isEmpty();
    assertThat(p.getId()).isNull();
  }

  @Test
  void slotConstructorAndAccessors() {
    BlackboardEntity bb = new BlackboardEntity("bb");
    PanelEntity p = new PanelEntity(bb, "p", "d");
    URI uri = URI.create("classpath:schemas/passthrough.json");
    ContributionSlotEntity s = new ContributionSlotEntity(p, "s", uri);
    assertThat(s.getName()).isEqualTo("s");
    assertThat(s.getPanel()).isSameAs(p);
    assertThat(s.getSchemaUri()).isEqualTo(uri);
    assertThat(s.getContributions()).isEmpty();
  }

  @Test
  void contributionConstructorAndAccessors() {
    JsonNode node = JsonNodeFactory.instance.objectNode().put("k", "v");
    BlackboardEntity bb = new BlackboardEntity("bb");
    PanelEntity p = new PanelEntity(bb, "p", "d");
    ContributionSlotEntity s = new ContributionSlotEntity(p, "s", URI.create("classpath:schemas/passthrough.json"));
    ContributionEntity c = new ContributionEntity(s, node, null);
    assertThat(c.getContributionSlot()).isSameAs(s);
    assertThat(c.getPost()).isEqualTo(node);
    assertThat(c.getDerivedContributions()).isEmpty();
    ContributionEntity c2 = new ContributionEntity(s, node, List.of(c));
    assertThat(c2.getDerivedContributions()).hasSize(1);
  }

  @Test
  void equalsAndHashCodeForAllEntities() {
    BlackboardEntity bb = new BlackboardEntity("bb");
    assertThat(bb).isEqualTo(bb).isNotEqualTo(null).isNotEqualTo("x");
    assertThat(bb.hashCode()).isNotZero();

    PanelEntity p = new PanelEntity(bb, "p", "d");
    assertThat(p).isEqualTo(p).isNotEqualTo(null).isNotEqualTo("x");
    assertThat(p.hashCode()).isNotZero();

    ContributionSlotEntity s = new ContributionSlotEntity(p, "s", URI.create("classpath:schemas/passthrough.json"));
    assertThat(s).isEqualTo(s).isNotEqualTo(null).isNotEqualTo("x");
    assertThat(s.hashCode()).isNotZero();

    ContributionEntity c = new ContributionEntity(s, JsonNodeFactory.instance.objectNode(), null);
    assertThat(c).isEqualTo(c).isNotEqualTo(null).isNotEqualTo("x");
    assertThat(c.hashCode()).isNotZero();
  }
}
