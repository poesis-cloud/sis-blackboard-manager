package cloud.poesis.sie.bbman.repository;

import cloud.poesis.sie.bbman.entity.ContributionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link ContributionEntity}.
 *
 * <p>Per the Repository–Service exclusivity rule this repository is consumed exclusively by
 * {@link cloud.poesis.sie.bbman.service.ContributionService}.
 *
 * <p>Sealed-stream order is byte-stable: {@code (panel.name, slot.name, timestamp, id)}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public interface ContributionRepository extends JpaRepository<ContributionEntity, UUID> {

  @Query(
      "select c from ContributionEntity c "
          + "join c.contributionSlot s join s.panel p "
          + "where p.blackboard.id = :blackboardId "
          + "order by p.name asc, s.name asc, c.timestamp asc, c.id asc")
  List<ContributionEntity> findByBlackboardIdOrderedByteStable(UUID blackboardId);

  @Query(
      "select c from ContributionEntity c "
          + "join c.contributionSlot s join s.panel p "
          + "where p.blackboard.id = :blackboardId "
          + "  and (:panelName is null or p.name = :panelName) "
          + "  and (:slotName is null or s.name = :slotName) "
          + "order by p.name asc, s.name asc, c.timestamp asc, c.id asc")
  List<ContributionEntity> findByBlackboardIdFiltered(
      UUID blackboardId, String panelName, String slotName);

  @Query(
      "select count(c) from ContributionEntity c "
          + "join c.contributionSlot s join s.panel p "
          + "where p.blackboard.id = :blackboardId")
  long countByBlackboardId(UUID blackboardId);
}
