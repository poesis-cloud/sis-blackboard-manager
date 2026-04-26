package cloud.poesis.sie.bbman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cloud.poesis.sie.bbman.entity.BlackboardEntity;
import cloud.poesis.sie.bbman.entity.PanelEntity;
import cloud.poesis.sie.bbman.exception.ResourceNotFoundException;
import cloud.poesis.sie.bbman.exception.RuleViolationException;
import cloud.poesis.sie.bbman.type.BlackboardConsistencyRuleType;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServicesUnitCoverageTest {

  @Autowired BlackboardService blackboardService;
  @Autowired PanelService panelService;
  @Autowired ContributionSlotService slotService;

  private static final URI SCHEMA = URI.create("classpath:schemas/passthrough.json");

  @Test
  void blackboardServiceGetByIdNotFoundThrows() {
    assertThatThrownBy(() -> blackboardService.getById(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void panelServiceGetByIdNotFoundThrows() {
    assertThatThrownBy(() -> panelService.getById(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void panelServiceGetByIdWithSlotsNotFoundThrows() {
    assertThatThrownBy(() -> panelService.getByIdWithSlots(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void panelServiceListByBlackboard() {
    BlackboardEntity bb = blackboardService.create("bb-list-panels-" + UUID.randomUUID());
    panelService.declare(bb.getId(), "p", "desc");
    assertThat(panelService.listByBlackboard(bb.getId())).hasSize(1);
  }

  @Test
  void panelServiceBlankPanelNameRejected() {
    BlackboardEntity bb = blackboardService.create("bb-blank-name-" + UUID.randomUUID());
    assertThatThrownBy(() -> panelService.declare(bb.getId(), "  ", "desc"))
        .isInstanceOf(RuleViolationException.class)
        .hasMessageContaining("required");
  }

  @Test
  void panelServiceNullPanelNameRejected() {
    BlackboardEntity bb = blackboardService.create("bb-null-name-" + UUID.randomUUID());
    assertThatThrownBy(() -> panelService.declare(bb.getId(), null, "desc"))
        .isInstanceOf(RuleViolationException.class);
  }

  @Test
  void panelServiceResolveByNameUnknownThrows422() {
    BlackboardEntity bb = blackboardService.create("bb-resolve-" + UUID.randomUUID());
    RuleViolationException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            RuleViolationException.class,
            () -> panelService.resolveByName(bb.getId(), "missing"));
    assertThat(ex.getRuleType())
        .isEqualTo(BlackboardConsistencyRuleType.CONTRIBUTION_PANEL_NOT_FOUND);
  }

  @Test
  void slotServiceLifecycle() {
    BlackboardEntity bb = blackboardService.create("bb-slots-" + UUID.randomUUID());
    PanelEntity p = panelService.declare(bb.getId(), "p", "d");
    var slot = slotService.declare(p.getId(), "s", SCHEMA);
    assertThat(slotService.getById(slot.getId()).getName()).isEqualTo("s");
    assertThat(slotService.listByPanel(p.getId())).hasSize(1);
    assertThat(slotService.resolveByName(p.getId(), "s").getId()).isEqualTo(slot.getId());
  }

  @Test
  void slotServiceGetByIdNotFoundThrows() {
    assertThatThrownBy(() -> slotService.getById(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void slotServiceDuplicateNameRejected() {
    BlackboardEntity bb = blackboardService.create("bb-dup-slot-" + UUID.randomUUID());
    PanelEntity p = panelService.declare(bb.getId(), "p", "d");
    slotService.declare(p.getId(), "s", SCHEMA);
    RuleViolationException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            RuleViolationException.class, () -> slotService.declare(p.getId(), "s", SCHEMA));
    assertThat(ex.getRuleType())
        .isEqualTo(BlackboardConsistencyRuleType.SLOT_NAME_NOT_UNIQUE_IN_PANEL);
  }

  @Test
  void slotServiceUnresolvableSchemaRejected() {
    BlackboardEntity bb = blackboardService.create("bb-bad-schema-" + UUID.randomUUID());
    PanelEntity p = panelService.declare(bb.getId(), "p", "d");
    RuleViolationException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            RuleViolationException.class,
            () ->
                slotService.declare(
                    p.getId(), "s", URI.create("classpath:schemas/missing.json")));
    assertThat(ex.getRuleType())
        .isEqualTo(BlackboardConsistencyRuleType.SLOT_SCHEMA_URI_UNRESOLVABLE);
  }

  @Test
  void slotServiceResolveByNameUnknownThrows422() {
    BlackboardEntity bb = blackboardService.create("bb-resolve-slot-" + UUID.randomUUID());
    PanelEntity p = panelService.declare(bb.getId(), "p", "d");
    RuleViolationException ex =
        org.junit.jupiter.api.Assertions.assertThrows(
            RuleViolationException.class,
            () -> slotService.resolveByName(p.getId(), "missing"));
    assertThat(ex.getRuleType())
        .isEqualTo(BlackboardConsistencyRuleType.CONTRIBUTION_SLOT_NOT_FOUND);
  }

  @Test
  void slotServiceListByPanelUnknownPanelThrows() {
    assertThatThrownBy(() -> slotService.listByPanel(UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
