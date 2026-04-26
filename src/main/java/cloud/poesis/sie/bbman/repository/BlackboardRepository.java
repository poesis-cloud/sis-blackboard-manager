package cloud.poesis.sie.bbman.repository;

import cloud.poesis.sie.bbman.entity.BlackboardEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link BlackboardEntity}.
 *
 * <p>Per the Repository–Service exclusivity rule this repository is consumed exclusively by
 * {@link cloud.poesis.sie.bbman.service.BlackboardService}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public interface BlackboardRepository extends JpaRepository<BlackboardEntity, UUID> {}
