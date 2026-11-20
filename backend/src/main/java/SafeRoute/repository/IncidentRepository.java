package SafeRoute.repository;

import SafeRoute.entity.Incident;
import SafeRoute.enums.IncidentStatus;
import SafeRoute.enums.IncidentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByIncidentType(IncidentType incidentType);
    List<Incident> findByUserId(Long userId);
}

