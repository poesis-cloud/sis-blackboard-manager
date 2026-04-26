package cloud.poesis.sie.bbman.service;

import cloud.poesis.sie.bbman.entity.ContributionEntity;
import cloud.poesis.sie.bbman.entity.ContributionSlotEntity;
import cloud.poesis.sie.bbman.entity.PanelEntity;
import cloud.poesis.sie.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.repository.ContributionRepository;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sie.bbman.type.PrimitiveType;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns {@link ContributionRepository}. Validates {@code post} against the slot's JSON Schema and
 * asserts derived-contribution lineage stays within the same Blackboard.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Service
@Transactional("transactionManager")
public class ContributionService {

  private final ContributionRepository contributionRepository;
  private final BlackboardService blackboardService;
  private final PanelService panelService;
  private final ContributionSlotService slotService;
  private final JsonSchemaValidationService jsonSchemaValidationService;

  public ContributionService(
      ContributionRepository contributionRepository,
      BlackboardService blackboardService,
      @Lazy PanelService panelService,
      ContributionSlotService slotService,
      JsonSchemaValidationService jsonSchemaValidationService) {
    this.contributionRepository = contributionRepository;
    this.blackboardService = blackboardService;
    this.panelService = panelService;
    this.slotService = slotService;
    this.jsonSchemaValidationService = jsonSchemaValidationService;
  }

  /**
   * Posts a new Contribution on an OPEN Blackboard. Validates the {@code post} payload against the
   * slot schema and rejects cross-Blackboard derived references.
   *
   * @param blackboardId the parent Blackboard id
   * @param panelName the panel name
   * @param slotName the slot name
   * @param post the JSON payload
   * @param derivedIds optional in-Blackboard contribution lineage ids; may be {@code null}
   * @return the persisted contribution
   */
  public ContributionEntity post(
      UUID blackboardId,
      String panelName,
      String slotName,
      JsonNode post,
      List<UUID> derivedIds) {
    blackboardService.getOpenOrFail(blackboardId);
    PanelEntity panel = panelService.resolveByName(blackboardId, panelName);
    ContributionSlotEntity slot = slotService.resolveByName(panel.getId(), slotName);

    jsonSchemaValidationService.validate(slot.getSchemaUri(), post);

    List<ContributionEntity> derived = resolveDerived(blackboardId, derivedIds);

    ContributionEntity saved = contributionRepository.save(new ContributionEntity(slot, post, derived));
    // Force initialization of lazy associations needed for DTO mapping (open-in-view=false).
    saved.getContributionSlot().getPanel().getName();
    saved.getDerivedContributions().size();
    return saved;
  }

  private List<ContributionEntity> resolveDerived(UUID blackboardId, List<UUID> derivedIds) {
    if (derivedIds == null || derivedIds.isEmpty()) {
      return new ArrayList<>();
    }
    List<ContributionEntity> resolved = new ArrayList<>(derivedIds.size());
    for (UUID id : derivedIds) {
      ContributionEntity ref =
          contributionRepository
              .findById(id)
              .orElseThrow(
                  () ->
                      RuleViolationException.of(
                          BlackboardConsistencyRuleType.CONTRIBUTION_DERIVED_NOT_FOUND,
                          "Derived contribution " + id + " not found.",
                          "contributionId", id));
      UUID refBlackboardId =
          ref.getContributionSlot().getPanel().getBlackboard().getId();
      if (!refBlackboardId.equals(blackboardId)) {
        throw RuleViolationException.of(
            BlackboardConsistencyRuleType.CONTRIBUTION_DERIVED_CROSS_BLACKBOARD,
            "Derived contribution " + id + " belongs to a different Blackboard.",
            "contributionId", id,
            "blackboardId", blackboardId,
            "foreignBlackboardId", refBlackboardId);
      }
      resolved.add(ref);
    }
    return resolved;
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public ContributionEntity getById(UUID id) {
    return contributionRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(PrimitiveType.CONTRIBUTION, id));
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public List<ContributionEntity> listByBlackboard(
      UUID blackboardId, String panelName, String slotName) {
    blackboardService.getById(blackboardId);
    List<ContributionEntity> list;
    if (panelName == null && slotName == null) {
      list = contributionRepository.findByBlackboardIdOrderedByteStable(blackboardId);
    } else {
      list = contributionRepository.findByBlackboardIdFiltered(blackboardId, panelName, slotName);
    }
    for (ContributionEntity c : list) {
      c.getContributionSlot().getPanel().getName();
      c.getDerivedContributions().size();
    }
    return list;
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public long countByBlackboardId(UUID blackboardId) {
    return contributionRepository.countByBlackboardId(blackboardId);
  }
}
