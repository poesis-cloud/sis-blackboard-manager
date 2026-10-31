package cloud.poesis.sis.bbman.controller;

import cloud.poesis.sis.bbman.dto.ContributionDto;
import cloud.poesis.sis.bbman.dto.ContributionPostDto;
import cloud.poesis.sis.bbman.entity.ContributionEntity;
import cloud.poesis.sis.bbman.service.ContributionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contribution REST endpoints under
 * {@code /api/v1/blackboards/{bbId}/contributions}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/blackboards/{blackboardId}/contributions")
@Tag(name = "Contributions", description = "Post and read contributions on a Blackboard")
public class ContributionController extends AbstractController {

  private final ContributionService contributionService;

  public ContributionController(ContributionService contributionService) {
    this.contributionService = contributionService;
  }

  @PostMapping
  public ResponseEntity<ContributionDto> post(
      @PathVariable UUID blackboardId, @Valid @RequestBody ContributionPostDto body) {
    ContributionEntity c = contributionService.post(
        blackboardId, body.panel(), body.slot(), body.post(), body.derivedContribution());
    return ResponseEntity.created(
        URI.create("/api/v1/blackboards/" + blackboardId + "/contributions/" + c.getId()))
        .body(ContributionDto.from(c));
  }

  @GetMapping
  public List<ContributionDto> list(
      @PathVariable UUID blackboardId,
      @RequestParam(required = false) String panel,
      @RequestParam(required = false) String slot) {
    return contributionService.listByBlackboard(blackboardId, panel, slot).stream()
        .map(ContributionDto::from)
        .toList();
  }
}
