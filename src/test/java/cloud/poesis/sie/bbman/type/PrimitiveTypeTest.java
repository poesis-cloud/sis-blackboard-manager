package cloud.poesis.sie.bbman.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PrimitiveTypeTest {

  @Test
  void valuesAndLabels() {
    assertThat(PrimitiveType.BLACKBOARD.getValue()).isEqualTo("blackboard");
    assertThat(PrimitiveType.BLACKBOARD.getLabel()).isEqualTo("Blackboard");
    assertThat(PrimitiveType.PANEL.getValue()).isEqualTo("panel");
    assertThat(PrimitiveType.CONTRIBUTION_SLOT.getValue()).isEqualTo("contribution-slot");
    assertThat(PrimitiveType.CONTRIBUTION.getValue()).isEqualTo("contribution");
  }

  @Test
  void fromValueResolvesCaseInsensitively() {
    assertThat(PrimitiveType.fromValue("Blackboard")).isEqualTo(PrimitiveType.BLACKBOARD);
    assertThat(PrimitiveType.fromValue("contribution-slot"))
        .isEqualTo(PrimitiveType.CONTRIBUTION_SLOT);
  }

  @Test
  void fromValueThrowsOnUnknown() {
    assertThatThrownBy(() -> PrimitiveType.fromValue("nope"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
