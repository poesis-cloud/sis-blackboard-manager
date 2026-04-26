package cloud.poesis.sie.bbman.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;

/**
 * Request body for {@code POST /api/v1/blackboards/{id}/panels}.
 *
 * @param name        the client-namespaced panel name
 * @param description the free-form panel description (must be non-blank)
 * @param slots       the slots to declare under this panel
 * @author Clément Cazaud
 * @since 1.0.0
 */
public record PanelDeclarationDto(
    @NotBlank String name,
    @NotBlank String description,
    @NotEmpty @Valid List<SlotSpec> slots) {

  /**
   * Slot specification inside a panel declaration.
   *
   * @param name      the slot name (unique within its parent panel)
   * @param schemaUri the JSON Schema URI for {@code Contribution.post}
   */
  public record SlotSpec(@NotBlank String name, @NotNull URI schemaUri) {
  }
}
