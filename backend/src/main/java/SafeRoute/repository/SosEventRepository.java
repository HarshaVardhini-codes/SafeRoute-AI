package SafeRoute.repository;

import SafeRoute.entity.SosEvent;
import SafeRoute.enums.SosStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SosEventRepository extends JpaRepository<SosEvent, Long> {

    List<SosEvent> findByUserIdOrderByTriggeredAtDesc(Long userId);

    Optional<SosEvent> findFirstByUserIdAndStatusOrderByTriggeredAtDesc(Long userId, SosStatus status);

    boolean existsByUserIdAndStatus(Long userId, SosStatus status);

    List<SosEvent> findByStatus(SosStatus status);
}

