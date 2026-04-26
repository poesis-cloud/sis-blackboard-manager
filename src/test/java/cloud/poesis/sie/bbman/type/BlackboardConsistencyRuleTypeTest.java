package cloud.poesis.sie.bbman.type;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BlackboardConsistencyRuleTypeTest {

  @Test
  void typeIsKebabCasedAndPrefixed() {
    assertThat(BlackboardConsistencyRuleType.PANEL_TOPOLOGY_FROZEN.getType())
        .isEqualTo("bbm:rules/panel-topology-frozen");
  }

  @Test
  void titleAndDescriptionAreNonNull() {
    for (BlackboardConsistencyRuleType r : BlackboardConsistencyRuleType.values()) {
      assertThat(r.getTitle()).isNotBlank();
      assertThat(r.getDescription()).isNotBlank();
      assertThat(r.getType()).startsWith("bbm:rules/");
    }
  }
}
