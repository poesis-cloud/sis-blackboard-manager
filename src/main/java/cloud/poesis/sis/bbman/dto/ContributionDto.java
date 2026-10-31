package cloud.poesis.sis.bbman.dto;

import cloud.poesis.sis.bbman.entity.ContributionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing a Contribution.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record ContributionDto(
    UUID id,
    String panel,
    String slot,
    JsonNode post,
    Instant timestamp,
    List<UUID> derivedContribution) {

  public static ContributionDto from(ContributionEntity c) {
    return new ContributionDto(
        c.getId(),
        c.getContributionSlot().getPanel().getName(),
        c.getContributionSlot().getName(),
        c.getPost(),
        c.getTimestamp(),
        c.getDerivedContributions().stream().map(ContributionEntity::getId).toList());
  }
}
