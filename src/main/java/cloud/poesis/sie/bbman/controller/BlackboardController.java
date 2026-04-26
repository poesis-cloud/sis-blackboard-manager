package cloud.poesis.sie.bbman.controller;

import cloud.poesis.sie.bbman.dto.BlackboardConfigDto;
import cloud.poesis.sie.bbman.dto.BlackboardDto;
import cloud.poesis.sie.bbman.dto.BlackboardStateDto;
import cloud.poesis.sie.bbman.dto.PanelDeclarationDto;
import cloud.poesis.sie.bbman.dto.PanelDto;
import cloud.poesis.sie.bbman.entity.BlackboardEntity;
import cloud.poesis.sie.bbman.entity.PanelEntity;
import cloud.poesis.sie.bbman.service.BlackboardService;
import cloud.poesis.sie.bbman.service.ContributionService;
import cloud.poesis.sie.bbman.service.ContributionSlotService;
import cloud.poesis.sie.bbman.service.PanelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Blackboard, Panel and ContributionSlot REST endpoints under
 * {@code /api/v1/blackboards}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/blackboards")
@Tag(name = "Blackboards", description = "Blackboard, Panel and Slot lifecycle operations")
public class BlackboardController extends AbstractController {

  private final BlackboardService blackboardService;
  private final PanelService panelService;
  private final ContributionSlotService slotService;
  private final ContributionService contributionService;

  public BlackboardController(
      BlackboardService blackboardService,
      PanelService panelService,
      ContributionSlotService slotService,
      ContributionService contributionService) {
    this.blackboardService = blackboardService;
    this.panelService = panelService;
    this.slotService = slotService;
    this.contributionService = contributionService;
  }

  @PostMapping
  public ResponseEntity<BlackboardDto> create(@Valid @RequestBody BlackboardConfigDto config) {
    BlackboardEntity bb = blackboardService.create(config.name());
    return ResponseEntity.created(URI.create("/api/v1/blackboards/" + bb.getId()))
        .body(BlackboardDto.from(bb));
  }

  @GetMapping("/{id}")
  public BlackboardDto get(@PathVariable UUID id) {
    return BlackboardDto.from(blackboardService.getById(id));
  }

  @PostMapping("/{id}/abort")
  public BlackboardDto abort(@PathVariable UUID id) {
    return BlackboardDto.from(blackboardService.abort(id));
  }

  @PostMapping("/{id}/seal")
  public BlackboardDto seal(@PathVariable UUID id) {
    return BlackboardDto.from(blackboardService.seal(id));
  }

  @GetMapping("/{id}/state")
  public BlackboardStateDto state(@PathVariable UUID id) {
    BlackboardEntity bb = blackboardService.getById(id);
    List<PanelDto> panels = panelService.listByBlackboardWithSlots(id).stream().map(PanelDto::from).toList();
    long count = contributionService.countByBlackboardId(id);
    return BlackboardStateDto.from(bb, panels, count);
  }

  @PostMapping("/{id}/panels")
  public ResponseEntity<PanelDto> declarePanel(
      @PathVariable UUID id, @Valid @RequestBody PanelDeclarationDto declaration) {
    PanelEntity panel = panelService.declare(id, declaration.name(), declaration.description());
    declaration
        .slots()
        .forEach(spec -> slotService.declare(panel.getId(), spec.name(), spec.schemaUri()));
    PanelEntity reloaded = panelService.getByIdWithSlots(panel.getId());
    return ResponseEntity.created(
        URI.create("/api/v1/blackboards/" + id + "/panels/" + panel.getId()))
        .body(PanelDto.from(reloaded));
  }

  @GetMapping("/{id}/panels")
  public List<PanelDto> listPanels(@PathVariable UUID id) {
    return panelService.listByBlackboardWithSlots(id).stream().map(PanelDto::from).toList();
  }
}
