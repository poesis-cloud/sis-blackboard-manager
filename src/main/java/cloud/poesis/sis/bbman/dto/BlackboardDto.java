package cloud.poesis.sis.bbman.dto;

import cloud.poesis.sis.bbman.entity.BlackboardEntity;
import cloud.poesis.sis.bbman.type.BlackboardStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing a Blackboard.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record BlackboardDto(
    UUID id, String name, Instant timestamp, BlackboardStatus status, Instant sealTimestamp) {

  public static BlackboardDto from(BlackboardEntity e) {
    return new BlackboardDto(
        e.getId(), e.getName(), e.getTimestamp(), e.getStatus(), e.getSealTimestamp());
  }
}
