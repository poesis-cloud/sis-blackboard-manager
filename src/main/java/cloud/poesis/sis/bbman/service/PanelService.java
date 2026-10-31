package cloud.poesis.sis.bbman.service;

import cloud.poesis.sis.bbman.entity.BlackboardEntity;
import cloud.poesis.sis.bbman.entity.PanelEntity;
import cloud.poesis.sis.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sis.bbman.exception.RuleViolationException;
import cloud.poesis.sis.bbman.repository.PanelRepository;
import cloud.poesis.sis.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sis.bbman.type.PrimitiveType;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns {@link PanelRepository}. Cross-type lookups (Blackboard, Contribution
 * count) go through the
 * owning service.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Service
@Transactional("transactionManager")
public class PanelService {

  private static final String[] RESERVED_PREFIXES = { "bb:", "_internal:" };

  private final PanelRepository panelRepository;
  private final BlackboardService blackboardService;
  private final ContributionService contributionService;

  public PanelService(
      PanelRepository panelRepository,
      BlackboardService blackboardService,
      @Lazy ContributionService contributionService) {
    this.panelRepository = panelRepository;
    this.blackboardService = blackboardService;
    this.contributionService = contributionService;
  }

  /**
   * Declares a new Panel under an OPEN Blackboard before any contribution has
   * been accepted on it.
   * Panel topology freezes once the first contribution lands.
   *
   * @param blackboardId the parent Blackboard id
   * @param name         the client-namespaced panel name
   * @param description  the free-form description (must be non-blank)
   * @return the persisted Panel
   */
  public PanelEntity declare(UUID blackboardId, String name, String description) {
    BlackboardEntity bb = blackboardService.getOpenOrFail(blackboardId);

    if (description == null || description.isBlank()) {
      throw new RuleViolationException(
          BlackboardConsistencyRuleType.PANEL_DESCRIPTION_REQUIRED,
          "Panel description must be non-empty.");
    }
    if (name == null || name.isBlank()) {
      throw new RuleViolationException(
          BlackboardConsistencyRuleType.PANEL_NAME_RESERVED_PREFIX, "Panel name is required.");
    }
    for (String prefix : RESERVED_PREFIXES) {
      if (name.startsWith(prefix)) {
        throw RuleViolationException.of(
            BlackboardConsistencyRuleType.PANEL_NAME_RESERVED_PREFIX,
            "Panel name '" + name + "' uses reserved prefix '" + prefix + "'.",
            "panelName", name, "prefix", prefix);
      }
    }
    if (contributionService.countByBlackboardId(blackboardId) > 0) {
      throw RuleViolationException.of(
          BlackboardConsistencyRuleType.PANEL_TOPOLOGY_FROZEN,
          "Cannot add panel: contributions already accepted on Blackboard " + blackboardId,
          "blackboardId", blackboardId);
    }
    return panelRepository.save(new PanelEntity(bb, name, description));
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public PanelEntity getById(UUID id) {
    return panelRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(PrimitiveType.PANEL, id));
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public List<PanelEntity> listByBlackboard(UUID blackboardId) {
    blackboardService.getById(blackboardId);
    return panelRepository.findByBlackboardId(blackboardId);
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public List<PanelEntity> listByBlackboardWithSlots(UUID blackboardId) {
    blackboardService.getById(blackboardId);
    return panelRepository.findByBlackboardIdWithSlots(blackboardId);
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public PanelEntity getByIdWithSlots(UUID id) {
    return panelRepository
        .findByIdWithSlots(id)
        .orElseThrow(() -> new ResourceNotFoundException(PrimitiveType.PANEL, id));
  }

  /**
   * Resolves a panel by Blackboard id and name, throwing a {@code 422} rule
   * violation if not found
   * (the call originates from a Contribution post path).
   *
   * @param blackboardId the parent Blackboard id
   * @param name         the panel name
   * @return the resolved panel
   */
  @Transactional(value = "transactionManager", readOnly = true)
  public PanelEntity resolveByName(UUID blackboardId, String name) {
    return panelRepository
        .findByBlackboardIdAndName(blackboardId, name)
        .orElseThrow(
            () -> RuleViolationException.of(
                BlackboardConsistencyRuleType.CONTRIBUTION_PANEL_NOT_FOUND,
                "Panel '" + name + "' not found on Blackboard " + blackboardId,
                "blackboardId", blackboardId, "panelName", name));
  }
}
