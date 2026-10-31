package cloud.poesis.sis.bbman.entity;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Client-declared topological unit grouping {@link ContributionSlotEntity}s
 * inside a
 * {@link BlackboardEntity}. Maps to the {@code panel} table.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Entity
@Table(name = "panel")
public class PanelEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false, insertable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "blackboard_id", nullable = false, updatable = false)
  private BlackboardEntity blackboard;

  @Column(name = "name", nullable = false, updatable = false)
  private String name;

  @Column(name = "description", nullable = false, updatable = false)
  private String description;

  @OneToMany(mappedBy = "panel", fetch = FetchType.LAZY)
  @OrderBy("name ASC")
  private List<ContributionSlotEntity> slots;

  protected PanelEntity() {
  }

  public PanelEntity(BlackboardEntity blackboard, String name, String description) {
    this.blackboard = Objects.requireNonNull(blackboard, "blackboard");
    this.name = Objects.requireNonNull(name, "name");
    this.description = Objects.requireNonNull(description, "description");
    this.slots = new ArrayList<>();
  }

  public UUID getId() {
    return id;
  }

  public BlackboardEntity getBlackboard() {
    return blackboard;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<ContributionSlotEntity> getSlots() {
    return slots;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    PanelEntity that = (PanelEntity) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
