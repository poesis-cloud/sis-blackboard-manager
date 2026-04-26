package cloud.poesis.sie.bbman.exception;

import cloud.poesis.sie.bbman.type.RuleType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic Blackboard rule-violation exception.
 *
 * <p>
 * Mapped to HTTP {@code 400}, {@code 409} or {@code 422} by the controller
 * layer depending on
 * {@link RuleType}, rendered as an RFC 9457 {@code ProblemDetail}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public class RuleViolationException extends RuntimeException {

  private final RuleType ruleType;
  private final Map<String, Object> site;

  public RuleViolationException(RuleType ruleType, String detail, Map<String, Object> site) {
    super(detail);
    this.ruleType = ruleType;
    this.site = site != null ? Map.copyOf(site) : Map.of();
  }

  public RuleViolationException(RuleType ruleType, String detail) {
    this(ruleType, detail, Map.of());
  }

  public RuleViolationException(
      RuleType ruleType, String detail, Map<String, Object> site, Throwable cause) {
    super(detail, cause);
    this.ruleType = ruleType;
    this.site = site != null ? Map.copyOf(site) : Map.of();
  }

  public RuleType getRuleType() {
    return ruleType;
  }

  public Map<String, Object> getSite() {
    return site;
  }

  public String getType() {
    return ruleType.getType();
  }

  public String getTitle() {
    return ruleType.getTitle();
  }

  public Map<String, Object> getExtensions() {
    Map<String, Object> ext = new LinkedHashMap<>(site);
    ext.put("rule", ruleType.name());
    ext.put("ruleDescription", ruleType.getDescription());
    return Map.copyOf(ext);
  }

  public static RuleViolationException of(
      RuleType ruleType, String detail, Object... siteKeyValuePairs) {
    return new RuleViolationException(ruleType, detail, toSite(siteKeyValuePairs));
  }

  public static RuleViolationException of(
      RuleType ruleType, String detail, Throwable cause, Object... siteKeyValuePairs) {
    return new RuleViolationException(ruleType, detail, toSite(siteKeyValuePairs), cause);
  }

  private static Map<String, Object> toSite(Object... keyValuePairs) {
    if (keyValuePairs.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Site key-value pairs must be even; got " + keyValuePairs.length);
    }
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < keyValuePairs.length; i += 2) {
      Object value = keyValuePairs[i + 1];
      if (value != null) {
        map.put((String) keyValuePairs[i], value);
      }
    }
    return Map.copyOf(map);
  }
}
