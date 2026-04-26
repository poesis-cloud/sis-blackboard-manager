package cloud.poesis.sie.bbman.repository;

import cloud.poesis.sie.bbman.entity.PanelEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Spring Data JPA repository for {@link PanelEntity}.
 *
 * <p>Per the Repository–Service exclusivity rule this repository is consumed exclusively by
 * {@link cloud.poesis.sie.bbman.service.PanelService}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public interface PanelRepository extends JpaRepository<PanelEntity, UUID> {

  List<PanelEntity> findByBlackboardId(UUID blackboardId);

  Optional<PanelEntity> findByBlackboardIdAndName(UUID blackboardId, String name);

  @Query(
      "select distinct p from PanelEntity p left join fetch p.slots where p.id = :id")
  Optional<PanelEntity> findByIdWithSlots(UUID id);

  @Query(
      "select distinct p from PanelEntity p left join fetch p.slots "
          + "where p.blackboard.id = :blackboardId order by p.name asc")
  List<PanelEntity> findByBlackboardIdWithSlots(UUID blackboardId);
}
