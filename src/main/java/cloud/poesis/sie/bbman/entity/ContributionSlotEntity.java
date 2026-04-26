package cloud.poesis.sie.bbman.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed write surface inside a {@link PanelEntity}. Carries the JSON Schema URI used to validate
 * each {@link ContributionEntity}'s {@code post} payload. Maps to the {@code contribution_slot}
 * table.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Entity
@Table(name = "contribution_slot")
public class ContributionSlotEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false, insertable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "panel_id", nullable = false, updatable = false)
  private PanelEntity panel;

  @Column(name = "name", nullable = false, updatable = false)
  private String name;

  @Column(name = "schema_uri", nullable = false, updatable = false)
  private String schemaUri;

  @OneToMany(mappedBy = "contributionSlot", fetch = FetchType.LAZY)
  @OrderBy("timestamp ASC, id ASC")
  private List<ContributionEntity> contributions;

  protected ContributionSlotEntity() {}

  public ContributionSlotEntity(PanelEntity panel, String name, URI schemaUri) {
    this.panel = Objects.requireNonNull(panel, "panel");
    this.name = Objects.requireNonNull(name, "name");
    this.schemaUri = Objects.requireNonNull(schemaUri, "schemaUri").toString();
    this.contributions = new ArrayList<>();
  }

  public UUID getId() {
    return id;
  }

  public PanelEntity getPanel() {
    return panel;
  }

  public String getName() {
    return name;
  }

  public URI getSchemaUri() {
    return URI.create(schemaUri);
  }

  public List<ContributionEntity> getContributions() {
    return contributions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ContributionSlotEntity that = (ContributionSlotEntity) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
