package cloud.poesis.sie.bbman.type;

/**
 * Consistency rules enforced by the Blackboard Manager substrate.
 *
 * <p>Each rule maps to a deterministic HTTP status (see {@code AbstractController}). All rule
 * semantics derive from {@code def/blackboard/blackboard.puml} and {@code blackboard-lifecycle.puml}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public enum BlackboardConsistencyRuleType implements RuleType {

  // ---- Blackboard FSM (CONFLICT 409) ----
  BLACKBOARD_NOT_OPEN_FOR_WRITE("Write rejected: Blackboard is not OPEN."),
  BLACKBOARD_NOT_OPEN_FOR_SEAL("Seal rejected: Blackboard is not OPEN."),
  BLACKBOARD_NOT_OPEN_FOR_ABORT("Abort rejected: Blackboard is in a terminal state."),
  PANEL_TOPOLOGY_FROZEN(
      "Panel topology is frozen on this Blackboard (first contribution already accepted)."),

  // ---- Validation (BAD_REQUEST 400) ----
  BLACKBOARD_NAME_REQUIRED("Blackboard 'name' is required."),
  PANEL_NAME_RESERVED_PREFIX("Panel name uses a reserved prefix (bb:, _internal:)."),
  PANEL_DESCRIPTION_REQUIRED("Panel 'description' must be non-empty."),
  PANEL_SLOTS_REQUIRED("Panel must declare at least one contribution slot."),
  SLOT_NAME_NOT_UNIQUE_IN_PANEL("Slot 'name' must be unique within its parent Panel."),
  SLOT_SCHEMA_URI_UNRESOLVABLE("Slot 'schemaUri' could not be resolved."),

  // ---- Validation (UNPROCESSABLE_ENTITY 422) ----
  CONTRIBUTION_PANEL_NOT_FOUND("Referenced panel does not exist on this Blackboard."),
  CONTRIBUTION_SLOT_NOT_FOUND("Referenced slot does not exist on the named panel."),
  CONTRIBUTION_POST_SCHEMA_VIOLATION("Contribution 'post' does not validate against slot schema."),
  CONTRIBUTION_DERIVED_CROSS_BLACKBOARD(
      "'derivedContribution' references contributions outside this Blackboard."),
  CONTRIBUTION_DERIVED_NOT_FOUND("'derivedContribution' references unknown contribution(s).");

  private final String description;

  BlackboardConsistencyRuleType(String description) {
    this.description = description;
  }

  @Override
  public String getType() {
    return "bbm:rules/" + name().toLowerCase().replace('_', '-');
  }

  @Override
  public String getTitle() {
    return "Blackboard rule violation";
  }

  @Override
  public String getDescription() {
    return description;
  }
}
