package cloud.poesis.sie.bbman.service;

import cloud.poesis.sie.bbman.entity.BlackboardEntity;
import cloud.poesis.sie.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.repository.BlackboardRepository;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sie.bbman.type.BlackboardStatus;
import cloud.poesis.sie.bbman.type.PrimitiveType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the {@code Blackboard} entity and its lifecycle FSM (OPEN → SEALING → SEALED | ABORTED).
 * Owns {@link BlackboardRepository}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Service
@Transactional("transactionManager")
public class BlackboardService {

  private final BlackboardRepository blackboardRepository;

  public BlackboardService(BlackboardRepository blackboardRepository) {
    this.blackboardRepository = blackboardRepository;
  }

  /**
   * Creates a new Blackboard in {@code OPEN} status with the given client-supplied name.
   *
   * @param name client-supplied free-form label (must be non-blank)
   * @return the persisted blackboard
   */
  public BlackboardEntity create(String name) {
    if (name == null || name.isBlank()) {
      throw new RuleViolationException(
          BlackboardConsistencyRuleType.BLACKBOARD_NAME_REQUIRED, "Blackboard name is required.");
    }
    return blackboardRepository.save(new BlackboardEntity(name));
  }

  /**
   * Retrieves a Blackboard by id.
   *
   * @param id the blackboard id
   * @return the blackboard
   * @throws ResourceNotFoundException if no blackboard exists with the given id
   */
  @Transactional(value = "transactionManager", readOnly = true)
  public BlackboardEntity getById(UUID id) {
    return blackboardRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(PrimitiveType.BLACKBOARD, id));
  }

  /**
   * Asserts the Blackboard is {@code OPEN} (callable by other services for write-time gating).
   *
   * @param id the blackboard id
   * @throws RuleViolationException with rule {@code BLACKBOARD_NOT_OPEN_FOR_WRITE} (HTTP 409) if
   *     the blackboard is not OPEN
   */
  @Transactional(value = "transactionManager", readOnly = true)
  public BlackboardEntity getOpenOrFail(UUID id) {
    BlackboardEntity bb = getById(id);
    if (bb.getStatus() != BlackboardStatus.OPEN) {
      throw RuleViolationException.of(
          BlackboardConsistencyRuleType.BLACKBOARD_NOT_OPEN_FOR_WRITE,
          "Blackboard " + id + " is " + bb.getStatus() + ", not OPEN.",
          "blackboardId", id, "status", bb.getStatus());
    }
    return bb;
  }

  /**
   * Aborts an OPEN or SEALING Blackboard.
   *
   * @param id the blackboard id
   * @return the updated blackboard
   */
  public BlackboardEntity abort(UUID id) {
    BlackboardEntity bb = getById(id);
    BlackboardStatus s = bb.getStatus();
    if (s == BlackboardStatus.SEALED || s == BlackboardStatus.ABORTED) {
      throw RuleViolationException.of(
          BlackboardConsistencyRuleType.BLACKBOARD_NOT_OPEN_FOR_ABORT,
          "Cannot abort Blackboard " + id + " in terminal state " + s,
          "blackboardId", id, "status", s);
    }
    bb.setStatus(BlackboardStatus.ABORTED);
    return blackboardRepository.save(bb);
  }

  /**
   * Seals an OPEN Blackboard. Transitions OPEN → SEALING → SEALED in a single transaction (the
   * substrate has no async work; SEALING is immediately followed by SEALED). If the persistence
   * commit fails, the caller is expected to mark the blackboard as ABORTED.
   *
   * @param id the blackboard id
   * @return the sealed blackboard
   */
  public BlackboardEntity seal(UUID id) {
    BlackboardEntity bb = getById(id);
    if (bb.getStatus() != BlackboardStatus.OPEN) {
      throw RuleViolationException.of(
          BlackboardConsistencyRuleType.BLACKBOARD_NOT_OPEN_FOR_SEAL,
          "Cannot seal Blackboard " + id + " in state " + bb.getStatus(),
          "blackboardId", id, "status", bb.getStatus());
    }
    bb.setStatus(BlackboardStatus.SEALING);
    blackboardRepository.saveAndFlush(bb);
    bb.setStatus(BlackboardStatus.SEALED);
    bb.setSealTimestamp(Instant.now());
    return blackboardRepository.save(bb);
  }
}
