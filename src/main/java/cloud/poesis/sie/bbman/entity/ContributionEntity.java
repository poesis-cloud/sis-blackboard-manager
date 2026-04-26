package cloud.poesis.sie.bbman.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Single fact posted by a client KS or reducer against a
 * {@link ContributionSlotEntity} of an
 * {@code OPEN} {@link BlackboardEntity}. Immutable once persisted. Maps to the
 * {@code contribution}
 * table; the {@code derived_contribution} link table holds the optional
 * in-Blackboard contribution
 * lineage.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
@Entity
@Table(name = "contribution")
public class ContributionEntity {

  @Id
  @GeneratedValue
  @Column(name = "id", nullable = false, updatable = false, insertable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contribution_slot_id", nullable = false, updatable = false)
  private ContributionSlotEntity contributionSlot;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "post", nullable = false, updatable = false, columnDefinition = "jsonb")
  private JsonNode post;

  @Column(name = "\"timestamp\"", nullable = false, updatable = false, insertable = false)
  private Instant timestamp;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "derived_contribution", joinColumns = @JoinColumn(name = "contribution_id"), inverseJoinColumns = @JoinColumn(name = "derived_id"))
  private List<ContributionEntity> derivedContributions;

  protected ContributionEntity() {
  }

  public ContributionEntity(
      ContributionSlotEntity contributionSlot,
      JsonNode post,
      List<ContributionEntity> derivedContributions) {
    this.contributionSlot = Objects.requireNonNull(contributionSlot, "contributionSlot");
    this.post = Objects.requireNonNull(post, "post");
    this.derivedContributions = derivedContributions == null ? new ArrayList<>()
        : new ArrayList<>(derivedContributions);
  }

  public UUID getId() {
    return id;
  }

  public ContributionSlotEntity getContributionSlot() {
    return contributionSlot;
  }

  public JsonNode getPost() {
    return post;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public List<ContributionEntity> getDerivedContributions() {
    return derivedContributions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ContributionEntity that = (ContributionEntity) o;
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
