package cloud.poesis.sis.bbman.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cloud.poesis.sis.bbman.entity.BlackboardEntity;

/**
 * Spring Data JPA repository for {@link BlackboardEntity}.
 *
 * <p>
 * Per the Repository–Service exclusivity rule this repository is consumed
 * exclusively by
 * {@link cloud.poesis.sis.bbman.service.BlackboardService}.
 *
 * @author Clément Cazaud
 * @since 1.0.0
 */
public interface BlackboardRepository extends JpaRepository<BlackboardEntity, UUID> {
}
