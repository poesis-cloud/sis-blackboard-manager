package cloud.poesis.sie.bbman.exception;

import java.util.Map;

/**
 * Unexpected internal failure in the Blackboard Manager. Mapped to HTTP 500.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public class InternalException extends RuntimeException {

  public InternalException(String detail) {
    super(detail);
  }

  public InternalException(String detail, Throwable cause) {
    super(detail, cause);
  }

  public String getType() {
    return "bbm:exceptions/internal-error";
  }

  public String getTitle() {
    return "Internal server error";
  }

  public Map<String, Object> getExtensions() {
    return Map.of();
  }
}
