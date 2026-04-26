package cloud.poesis.sie.bbman.type;

/**
 * Lifecycle status of a {@code Blackboard}.
 *
 * <p>
 * See {@code def/blackboard/blackboard-lifecycle.puml} for the authoritative
 * FSM.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public enum BlackboardStatus {
  /** Writes accepted; reads return the live contribution view. */
  OPEN,
  /** Transient state during the seal transition; writes are rejected (409). */
  SEALING,
  /**
   * Terminal-success; contribution stream is byte-stable; audit ledger is closed.
   */
  SEALED,
  /**
   * Terminal-failure; reached from {@code OPEN} (client abort) or {@code SEALING}
   * (irrecoverable failure).
   */
  ABORTED
}
