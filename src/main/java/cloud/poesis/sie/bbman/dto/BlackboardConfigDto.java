package cloud.poesis.sie.bbman.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/blackboards}.
 *
 * @param name the client-supplied free-form Blackboard label
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record BlackboardConfigDto(@NotBlank String name) {
}
