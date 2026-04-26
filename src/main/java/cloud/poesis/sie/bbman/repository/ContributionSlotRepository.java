package cloud.poesis.sie.bbman.repository;

import cloud.poesis.sie.bbman.entity.ContributionSlotEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link ContributionSlotEntity}.
 *
 * <p>
 * Per the Repository–Service exclusivity rule this repository is consumed
 * exclusively by
 * {@link cloud.poesis.sie.bbman.service.ContributionSlotService}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public interface ContributionSlotRepository extends JpaRepository<ContributionSlotEntity, UUID> {

  List<ContributionSlotEntity> findByPanelId(UUID panelId);

  Optional<ContributionSlotEntity> findByPanelIdAndName(UUID panelId, String name);
}
