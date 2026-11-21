package SafeRoute.service;

import SafeRoute.dto.request.SosStatusUpdateRequest;
import SafeRoute.dto.request.SosTriggerRequest;
import SafeRoute.dto.response.NotifiedContactSummary;
import SafeRoute.dto.response.SosEventResponse;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.SosEvent;
import SafeRoute.entity.User;
import SafeRoute.enums.Role;
import SafeRoute.enums.SosStatus;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.repository.EmergencyContactRepository;
import SafeRoute.repository.SosEventRepository;
import SafeRoute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SosService {

    private final SosEventRepository sosEventRepository;
    private final UserRepository userRepository;
    private final EmergencyContactRepository emergencyContactRepository;
    private final EmergencyNotificationService emergencyNotificationService;

    @Transactional
    public SosEventResponse triggerSos(SosTriggerRequest request, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);

        // Prevent duplicate ACTIVE SOS events for the same user
        if (sosEventRepository.existsByUserIdAndStatus(user.getId(), SosStatus.ACTIVE)) {
            throw new IllegalStateException("An active SOS emergency is already in progress for this user. " +
                    "Please resolve or cancel the active emergency before triggering a new one.");
        }

        // Build SOS Event
        SosEvent event = SosEvent.builder()
                .user(user)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .message(request.getMessage() != null ? request.getMessage().trim() : null)
                .status(SosStatus.ACTIVE)
                .contactsNotifiedCount(0)
                .build();

        SosEvent savedEvent = sosEventRepository.save(event);

        // Emergency contact notification workflow
        List<EmergencyContact> contacts = emergencyContactRepository.findByUserId(user.getId());
        List<NotifiedContactSummary> notifiedSummaries = emergencyNotificationService.notifyContacts(savedEvent, contacts);

        // Update with actual verified notification count (zero if gateway not active or failed)
        long successfulCount = notifiedSummaries != null
                ? notifiedSummaries.stream().filter(s -> "SENT".equalsIgnoreCase(s.getNotificationStatus())).count()
                : 0;
        savedEvent.setContactsNotifiedCount((int) successfulCount);
        sosEventRepository.save(savedEvent);

        return mapToResponse(savedEvent, notifiedSummaries);
    }

    @Transactional
    public SosEventResponse updateSosStatus(Long id, SosStatusUpdateRequest request, String currentUserEmail) {
        SosEvent event = getSosEntityById(id);
        User currentUser = getUserByEmail(currentUserEmail);

        validateModificationOwnership(event, currentUser);

        // Must be transitioning to RESOLVED or CANCELLED
        if (request.getStatus() == SosStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot transition an SOS event back to ACTIVE status.");
        }

        event.setStatus(request.getStatus());
        event.setResolvedAt(LocalDateTime.now());

        SosEvent updated = sosEventRepository.save(event);
        return mapToResponse(updated, Collections.emptyList());
    }

    public SosEventResponse getActiveSos(String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        Optional<SosEvent> activeEvent = sosEventRepository.findFirstByUserIdAndStatusOrderByTriggeredAtDesc(
                user.getId(), SosStatus.ACTIVE);

        return activeEvent.map(event -> mapToResponse(event, Collections.emptyList())).orElse(null);
    }

    public List<SosEventResponse> getSosHistory(String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        List<SosEvent> events = sosEventRepository.findByUserIdOrderByTriggeredAtDesc(user.getId());

        return events.stream()
                .map(event -> mapToResponse(event, Collections.emptyList()))
                .toList();
    }

    public SosEventResponse getSosEventById(Long id, String currentUserEmail) {
        SosEvent event = getSosEntityById(id);
        User currentUser = getUserByEmail(currentUserEmail);

        validateReadOwnership(event, currentUser);

        return mapToResponse(event, Collections.emptyList());
    }

    private SosEvent getSosEntityById(Long id) {
        return sosEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SOS event not found with id: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateModificationOwnership(SosEvent event, User user) {
        boolean isOwner = event.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to modify this SOS event");
        }
    }

    private void validateReadOwnership(SosEvent event, User user) {
        boolean isOwner = event.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to view this SOS event");
        }
    }

    private SosEventResponse mapToResponse(SosEvent event, List<NotifiedContactSummary> notifiedSummaries) {
        String googleMapsUrl = String.format("https://maps.google.com/?q=%f,%f",
                event.getLatitude(), event.getLongitude());

        return SosEventResponse.builder()
                .id(event.getId())
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .userFullName(event.getUser() != null ? event.getUser().getFullName() : null)
                .userPhoneNumber(event.getUser() != null ? event.getUser().getPhoneNumber() : null)
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .message(event.getMessage())
                .status(event.getStatus())
                .triggeredAt(event.getTriggeredAt())
                .resolvedAt(event.getResolvedAt())
                .contactsNotifiedCount(event.getContactsNotifiedCount())
                .googleMapsUrl(googleMapsUrl)
                .notifiedContacts(notifiedSummaries != null && !notifiedSummaries.isEmpty() ? notifiedSummaries : null)
                .build();
    }
}

