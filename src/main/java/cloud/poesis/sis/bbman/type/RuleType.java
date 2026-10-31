package cloud.poesis.sis.bbman.type;

/**
 * Marker interface for all Blackboard Manager rule types.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public interface RuleType {

  /**
   * Returns the enum constant name (provided by {@link Enum#name()}).
   *
   * @return the enum constant name; never {@code null}
   */
  String name();

  /**
   * Returns the stable Problem Details {@code type} URI for this rule.
   *
   * @return the rule type URI; never {@code null}
   */
  String getType();

  /**
   * Returns the human-readable Problem Details {@code title} for this rule.
   *
   * @return the rule title; never {@code null}
   */
  String getTitle();

  /**
   * Returns the longer human-readable description of this rule.
   *
   * @return the rule description; never {@code null}
   */
  String getDescription();
}
