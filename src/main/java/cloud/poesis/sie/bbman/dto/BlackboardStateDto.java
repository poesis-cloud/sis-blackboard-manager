package cloud.poesis.sie.bbman.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import cloud.poesis.sie.bbman.entity.BlackboardEntity;
import cloud.poesis.sie.bbman.type.BlackboardStatus;

/**
 * Response payload representing the full state of a Blackboard.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record BlackboardStateDto(
    UUID id,
    String name,
    BlackboardStatus status,
    Instant timestamp,
    Instant sealTimestamp,
    List<PanelDto> panels,
    long contributionCount) {

  public static BlackboardStateDto from(
      BlackboardEntity bb, List<PanelDto> panels, long contributionCount) {
    return new BlackboardStateDto(
        bb.getId(),
        bb.getName(),
        bb.getStatus(),
        bb.getTimestamp(),
        bb.getSealTimestamp(),
        panels,
        contributionCount);
  }
}
