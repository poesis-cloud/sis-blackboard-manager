package cloud.poesis.sie.bbman.controller;

import cloud.poesis.sie.bbman.exception.InternalException;
import cloud.poesis.sie.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sie.bbman.type.RuleType;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

/**
 * Common exception → RFC 9457 {@code ProblemDetail} mapping shared by all Blackboard Manager
 * controllers.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@RestController
public abstract class AbstractController {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    pd.setType(URI.create(ex.getType()));
    pd.setTitle(ex.getTitle());
    ex.getExtensions().forEach(pd::setProperty);
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
  }

  @ExceptionHandler(RuleViolationException.class)
  public ResponseEntity<ProblemDetail> handleRuleViolation(RuleViolationException ex) {
    HttpStatus status = mapRuleToHttpStatus(ex.getRuleType());
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    pd.setType(URI.create(ex.getType()));
    pd.setTitle(ex.getTitle());
    ex.getExtensions().forEach(pd::setProperty);
    return ResponseEntity.status(status).body(pd);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body validation failed.");
    pd.setType(URI.create("bbm:exceptions/validation-error"));
    pd.setTitle("Validation error");
    Map<String, Object> fields = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(fe -> fields.put(fe.getField(), fe.getDefaultMessage()));
    pd.setProperty("fields", fields);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setType(URI.create("bbm:exceptions/validation-error"));
    pd.setTitle("Validation error");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    pd.setType(URI.create("bbm:exceptions/illegal-argument"));
    pd.setTitle("Illegal argument");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
  }

  @ExceptionHandler(InternalException.class)
  public ResponseEntity<ProblemDetail> handleInternal(InternalException ex) {
    LOG.error("Internal error", ex);
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    pd.setType(URI.create("bbm:exceptions/internal-error"));
    pd.setTitle("Internal error");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleAny(Exception ex) {
    LOG.error("Unhandled error", ex);
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    pd.setType(URI.create("bbm:exceptions/internal-error"));
    pd.setTitle("Internal error");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
  }

  /**
   * Maps a rule type to its canonical HTTP status.
   *
   * @param ruleType the rule type
   * @return the HTTP status
   */
  protected HttpStatus mapRuleToHttpStatus(RuleType ruleType) {
    if (!(ruleType instanceof BlackboardConsistencyRuleType r)) {
      return HttpStatus.BAD_REQUEST;
    }
    return switch (r) {
      case BLACKBOARD_NOT_OPEN_FOR_WRITE,
          BLACKBOARD_NOT_OPEN_FOR_SEAL,
          BLACKBOARD_NOT_OPEN_FOR_ABORT,
          PANEL_TOPOLOGY_FROZEN -> HttpStatus.CONFLICT;
      case CONTRIBUTION_PANEL_NOT_FOUND,
          CONTRIBUTION_SLOT_NOT_FOUND,
          CONTRIBUTION_POST_SCHEMA_VIOLATION,
          CONTRIBUTION_DERIVED_CROSS_BLACKBOARD,
          CONTRIBUTION_DERIVED_NOT_FOUND -> HttpStatus.UNPROCESSABLE_ENTITY;
      default -> HttpStatus.BAD_REQUEST;
    };
  }
}
