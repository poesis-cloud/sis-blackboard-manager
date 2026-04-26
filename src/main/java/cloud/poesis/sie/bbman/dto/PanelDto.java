package cloud.poesis.sie.bbman.dto;

import cloud.poesis.sie.bbman.entity.PanelEntity;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing a Panel and its slots.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record PanelDto(UUID id, String name, String description, List<ContributionSlotDto> slots) {

  public static PanelDto from(PanelEntity p) {
    return new PanelDto(
        p.getId(),
        p.getName(),
        p.getDescription(),
        p.getSlots().stream().map(ContributionSlotDto::from).toList());
  }
}
