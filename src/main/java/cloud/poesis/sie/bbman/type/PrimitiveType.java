package cloud.poesis.sie.bbman.type;

/**
 * Canonical primitive/resource types used by Blackboard Manager entities and diagnostics.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public enum PrimitiveType {
  BLACKBOARD("blackboard", "Blackboard"),
  PANEL("panel", "Panel"),
  CONTRIBUTION_SLOT("contribution-slot", "ContributionSlot"),
  CONTRIBUTION("contribution", "Contribution");

  private final String value;
  private final String label;

  PrimitiveType(String value, String label) {
    this.value = value;
    this.label = label;
  }

  /**
   * Returns the machine-readable lowercase value.
   *
   * @return the lowercase identifier; never {@code null}
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the human-readable label.
   *
   * @return the display label; never {@code null}
   */
  public String getLabel() {
    return label;
  }

  /**
   * Resolves a primitive type from its lowercase value.
   *
   * @param value the lowercase string to resolve
   * @return the matching primitive type
   * @throws IllegalArgumentException if no constant matches {@code value}
   */
  public static PrimitiveType fromValue(String value) {
    for (PrimitiveType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown primitive_type: " + value);
  }
}
