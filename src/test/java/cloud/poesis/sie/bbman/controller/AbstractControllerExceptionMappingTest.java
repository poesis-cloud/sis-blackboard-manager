package cloud.poesis.sie.bbman.controller;

import static org.assertj.core.api.Assertions.assertThat;

import cloud.poesis.sie.bbman.exception.InternalException;
import cloud.poesis.sie.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sie.bbman.type.PrimitiveType;
import cloud.poesis.sie.bbman.type.RuleType;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class AbstractControllerExceptionMappingTest {

  private static class Stub extends AbstractController {
  }

  private final Stub controller = new Stub();

  @Test
  void resourceNotFoundMapsTo404() {
    var resp = controller.handleNotFound(
        new ResourceNotFoundException(PrimitiveType.BLACKBOARD, UUID.randomUUID()));
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(resp.getBody()).isNotNull();
  }

  @Test
  void ruleViolationMapsByCategory() {
    var conflict = controller.handleRuleViolation(
        new RuleViolationException(
            BlackboardConsistencyRuleType.PANEL_TOPOLOGY_FROZEN, "x"));
    assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

    var unprocessable = controller.handleRuleViolation(
        new RuleViolationException(
            BlackboardConsistencyRuleType.CONTRIBUTION_PANEL_NOT_FOUND, "x"));
    assertThat(unprocessable.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

    var badRequest = controller.handleRuleViolation(
        new RuleViolationException(
            BlackboardConsistencyRuleType.PANEL_NAME_RESERVED_PREFIX, "x"));
    assertThat(badRequest.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void unknownRuleTypeMapsTo400() {
    RuleType custom = new RuleType() {
      @Override
      public String name() {
        return "CUSTOM";
      }

      @Override
      public String getType() {
        return "x:y/z";
      }

      @Override
      public String getTitle() {
        return "t";
      }

      @Override
      public String getDescription() {
        return "d";
      }
    };
    var resp = controller.handleRuleViolation(new RuleViolationException(custom, "x"));
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void constraintViolation() {
    var resp = controller.handleConstraintViolation(new ConstraintViolationException("bad", null));
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void illegalArgument() {
    var resp = controller.handleIllegalArgument(new IllegalArgumentException("x"));
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void internalException() {
    var resp = controller.handleInternal(new InternalException("oops"));
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void anyException() {
    var resp = controller.handleAny(new RuntimeException("oops"));
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
