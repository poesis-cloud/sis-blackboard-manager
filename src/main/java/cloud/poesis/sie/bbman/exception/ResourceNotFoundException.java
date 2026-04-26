package cloud.poesis.sie.bbman.exception;

import cloud.poesis.sie.bbman.type.PrimitiveType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Exception thrown when a Blackboard resource lookup by identifier returns no
 * result.
 *
 * <p>
 * Mapped by the controller layer to HTTP {@code 404 Not Found} as RFC 9457
 * {@code ProblemDetail}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

  private final PrimitiveType resourceType;
  private final UUID resourceId;

  public ResourceNotFoundException(PrimitiveType resourceType, UUID resourceId) {
    super(resourceType.getLabel() + " " + resourceId + " not found");
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  public PrimitiveType getResourceType() {
    return resourceType;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getType() {
    return "bbm:exceptions/resource-not-found";
  }

  public String getTitle() {
    return "Not found";
  }

  public Map<String, Object> getExtensions() {
    Map<String, Object> map = new LinkedHashMap<>();
    if (resourceType != null) {
      map.put("resourceType", resourceType.getLabel());
    }
    if (resourceId != null) {
      map.put("resourceId", resourceId);
    }
    return Map.copyOf(map);
  }
}
