package cloud.poesis.sis.bbman.dto;

import cloud.poesis.sis.bbman.entity.ContributionSlotEntity;
import java.net.URI;
import java.util.UUID;

/**
 * Response payload representing a ContributionSlot.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record ContributionSlotDto(UUID id, String name, URI schemaUri) {

  public static ContributionSlotDto from(ContributionSlotEntity s) {
    return new ContributionSlotDto(s.getId(), s.getName(), s.getSchemaUri());
  }
}
