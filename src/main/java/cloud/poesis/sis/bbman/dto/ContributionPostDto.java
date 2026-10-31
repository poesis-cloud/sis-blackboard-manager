package cloud.poesis.sis.bbman.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/blackboards/{bbId}/contributions}.
 *
 * @param panel               the parent panel name
 * @param slot                the slot name within the panel
 * @param post                the JSON payload to validate against
 *                            {@code slot.schemaUri}
 * @param derivedContribution optional in-Blackboard contribution lineage ids
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record ContributionPostDto(
        @NotBlank String panel,
        @NotBlank String slot,
        @NotNull JsonNode post,
        List<UUID> derivedContribution) {
}
