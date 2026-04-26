package cloud.poesis.sie.bbman.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sie.bbman.type.PrimitiveType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExceptionTypesTest {

  @Test
  void resourceNotFound() {
    UUID id = UUID.randomUUID();
    ResourceNotFoundException ex = new ResourceNotFoundException(PrimitiveType.BLACKBOARD, id);
    assertThat(ex.getResourceType()).isEqualTo(PrimitiveType.BLACKBOARD);
    assertThat(ex.getResourceId()).isEqualTo(id);
    assertThat(ex.getType()).isEqualTo("bbm:exceptions/resource-not-found");
    assertThat(ex.getTitle()).isEqualTo("Not found");
    assertThat(ex.getExtensions()).containsEntry("resourceType", "Blackboard");
    assertThat(ex.getExtensions()).containsEntry("resourceId", id);
  }

  @Test
  void ruleViolationFactoryAndExtensions() {
    UUID bbId = UUID.randomUUID();
    RuleViolationException ex =
        RuleViolationException.of(
            BlackboardConsistencyRuleType.BLACKBOARD_NOT_OPEN_FOR_WRITE,
            "not open",
            "blackboardId", bbId);
    assertThat(ex.getRuleType())
        .isEqualTo(BlackboardConsistencyRuleType.BLACKBOARD_NOT_OPEN_FOR_WRITE);
    assertThat(ex.getMessage()).isEqualTo("not open");
    assertThat(ex.getSite()).containsEntry("blackboardId", bbId);
    assertThat(ex.getType()).isEqualTo("bbm:rules/blackboard-not-open-for-write");
    assertThat(ex.getTitle()).isEqualTo("Blackboard rule violation");
    assertThat(ex.getExtensions()).containsKeys("rule", "ruleDescription", "blackboardId");
  }

  @Test
  void ruleViolationFactoryWithCause() {
    Throwable cause = new IllegalStateException("boom");
    RuleViolationException ex =
        RuleViolationException.of(
            BlackboardConsistencyRuleType.SLOT_SCHEMA_URI_UNRESOLVABLE,
            "bad",
            cause,
            "key", "value");
    assertThat(ex.getCause()).isSameAs(cause);
    assertThat(ex.getSite()).containsEntry("key", "value");
  }

  @Test
  void ruleViolationOddPairsRejected() {
    assertThatThrownBy(
            () ->
                RuleViolationException.of(
                    BlackboardConsistencyRuleType.PANEL_NAME_RESERVED_PREFIX, "x", "k1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ruleViolationCtors() {
    RuleViolationException ex =
        new RuleViolationException(
            BlackboardConsistencyRuleType.PANEL_DESCRIPTION_REQUIRED, "x", Map.of("a", 1));
    assertThat(ex.getSite()).containsEntry("a", 1);
    RuleViolationException ex2 =
        new RuleViolationException(
            BlackboardConsistencyRuleType.PANEL_DESCRIPTION_REQUIRED, "x");
    assertThat(ex2.getSite()).isEmpty();
  }

  @Test
  void internalException() {
    InternalException ex = new InternalException("oops");
    assertThat(ex.getMessage()).isEqualTo("oops");
    assertThat(ex.getType()).isEqualTo("bbm:exceptions/internal-error");
    assertThat(ex.getTitle()).isEqualTo("Internal server error");
    assertThat(ex.getExtensions()).isEmpty();
    InternalException ex2 = new InternalException("oops", new RuntimeException("c"));
    assertThat(ex2.getCause()).isInstanceOf(RuntimeException.class);
  }
}
