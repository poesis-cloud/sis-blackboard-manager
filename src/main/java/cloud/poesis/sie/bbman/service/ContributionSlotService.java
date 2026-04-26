package cloud.poesis.sie.bbman.service;

import cloud.poesis.sie.bbman.entity.ContributionSlotEntity;
import cloud.poesis.sie.bbman.entity.PanelEntity;
import cloud.poesis.sie.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.repository.ContributionSlotRepository;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import cloud.poesis.sie.bbman.type.PrimitiveType;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns {@link ContributionSlotRepository}. Schema URI resolvability is asserted
 * at declare-time via
 * {@link JsonSchemaValidationService}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Service
@Transactional("transactionManager")
public class ContributionSlotService {

  private final ContributionSlotRepository slotRepository;
  private final PanelService panelService;
  private final JsonSchemaValidationService jsonSchemaValidationService;

  public ContributionSlotService(
      ContributionSlotRepository slotRepository,
      PanelService panelService,
      JsonSchemaValidationService jsonSchemaValidationService) {
    this.slotRepository = slotRepository;
    this.panelService = panelService;
    this.jsonSchemaValidationService = jsonSchemaValidationService;
  }

  /**
   * Declares a slot under the given Panel. Enforces unique name within the panel
   * and schema URI
   * resolvability.
   *
   * @param panelId   the parent Panel id
   * @param name      the slot name (unique within its panel)
   * @param schemaUri the JSON Schema URI for {@code Contribution.post}
   * @return the persisted slot
   */
  public ContributionSlotEntity declare(UUID panelId, String name, URI schemaUri) {
    PanelEntity panel = panelService.getById(panelId);
    if (slotRepository.findByPanelIdAndName(panelId, name).isPresent()) {
      throw RuleViolationException.of(
          BlackboardConsistencyRuleType.SLOT_NAME_NOT_UNIQUE_IN_PANEL,
          "Slot '" + name + "' already declared on panel " + panelId,
          "panelId", panelId, "slotName", name);
    }
    try {
      jsonSchemaValidationService.assertResolvable(schemaUri);
    } catch (RuntimeException e) {
      throw RuleViolationException.of(
          BlackboardConsistencyRuleType.SLOT_SCHEMA_URI_UNRESOLVABLE,
          "Slot schema URI '" + schemaUri + "' is not resolvable: " + e.getMessage(),
          e,
          "panelId", panelId, "slotName", name, "schemaUri", schemaUri);
    }
    return slotRepository.save(new ContributionSlotEntity(panel, name, schemaUri));
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public ContributionSlotEntity getById(UUID id) {
    return slotRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(PrimitiveType.CONTRIBUTION_SLOT, id));
  }

  @Transactional(value = "transactionManager", readOnly = true)
  public List<ContributionSlotEntity> listByPanel(UUID panelId) {
    panelService.getById(panelId);
    return slotRepository.findByPanelId(panelId);
  }

  /**
   * Resolves a slot by Panel id and name, throwing a {@code 422} rule violation
   * if not found.
   *
   * @param panelId the parent Panel id
   * @param name    the slot name
   * @return the resolved slot
   */
  @Transactional(value = "transactionManager", readOnly = true)
  public ContributionSlotEntity resolveByName(UUID panelId, String name) {
    return slotRepository
        .findByPanelIdAndName(panelId, name)
        .orElseThrow(
            () -> RuleViolationException.of(
                BlackboardConsistencyRuleType.CONTRIBUTION_SLOT_NOT_FOUND,
                "Slot '" + name + "' not found on panel " + panelId,
                "panelId", panelId, "slotName", name));
  }
}
