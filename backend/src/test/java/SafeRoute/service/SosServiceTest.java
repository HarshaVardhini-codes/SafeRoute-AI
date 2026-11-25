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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SosServiceTest {

    @Mock
    private SosEventRepository sosEventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private EmergencyNotificationService emergencyNotificationService;

    @InjectMocks
    private SosService sosService;

    private User regularUser;
    private User otherUser;
    private User adminUser;
    private SosEvent sampleEvent;
    private SosTriggerRequest triggerRequest;
    private EmergencyContact emergencyContact;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .fullName("Alice Smith")
                .email("alice@example.com")
                .phoneNumber("9876543210")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .fullName("Bob Jones")
                .email("bob@example.com")
                .phoneNumber("5551234567")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(99L)
                .fullName("Admin Officer")
                .email("admin@example.com")
                .phoneNumber("1112223333")
                .role(Role.ADMIN)
                .build();

        sampleEvent = SosEvent.builder()
                .id(100L)
                .user(regularUser)
                .latitude(12.9716)
                .longitude(77.5946)
                .message("Need help immediately!")
                .status(SosStatus.ACTIVE)
                .contactsNotifiedCount(1)
                .triggeredAt(LocalDateTime.now())
                .build();

        triggerRequest = SosTriggerRequest.builder()
                .latitude(12.9716)
                .longitude(77.5946)
                .message("Need help immediately!")
                .build();

        emergencyContact = EmergencyContact.builder()
                .id(10L)
                .contactName("Mom")
                .phoneNumber("9998887777")
                .relationship("Parent")
                .user(regularUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully trigger SOS and notify emergency contacts")
    void testTriggerSos_Success() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));
        when(sosEventRepository.existsByUserIdAndStatus(1L, SosStatus.ACTIVE)).thenReturn(false);
        when(sosEventRepository.save(any(SosEvent.class))).thenReturn(sampleEvent);
        when(emergencyContactRepository.findByUserId(1L)).thenReturn(Collections.singletonList(emergencyContact));

        NotifiedContactSummary summary = NotifiedContactSummary.builder()
                .contactName("Mom")
                .phoneNumber("9998887777")
                .relationship("Parent")
                .notificationStatus("SENT")
                .build();
        when(emergencyNotificationService.notifyContacts(any(), any())).thenReturn(Collections.singletonList(summary));

        SosEventResponse response = sosService.triggerSos(triggerRequest, "alice@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(SosStatus.ACTIVE, response.getStatus());
        assertEquals("Alice Smith", response.getUserFullName());
        assertEquals(1, response.getContactsNotifiedCount());
        assertTrue(response.getGoogleMapsUrl().contains("12.971600,77.594600"));
        verify(sosEventRepository, times(2)).save(any(SosEvent.class));
    }

    @Test
    @DisplayName("Should prevent duplicate ACTIVE SOS events for the same user")
    void testTriggerSos_DuplicateActive_ThrowsIllegalState() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));
        when(sosEventRepository.existsByUserIdAndStatus(1L, SosStatus.ACTIVE)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
                sosService.triggerSos(triggerRequest, "alice@example.com"));

        verify(sosEventRepository, never()).save(any(SosEvent.class));
    }

    @Test
    @DisplayName("Owner can resolve an active SOS event")
    void testUpdateSosStatus_OwnerCanResolve() {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.RESOLVED);
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));
        when(sosEventRepository.save(any(SosEvent.class))).thenReturn(sampleEvent);

        SosEventResponse response = sosService.updateSosStatus(100L, updateReq, "alice@example.com");

        assertNotNull(response);
        assertEquals(SosStatus.RESOLVED, sampleEvent.getStatus());
        assertNotNull(sampleEvent.getResolvedAt());
        verify(sosEventRepository).save(sampleEvent);
    }

    @Test
    @DisplayName("Admin can resolve another user's SOS event")
    void testUpdateSosStatus_AdminCanResolve() {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.RESOLVED);
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(sosEventRepository.save(any(SosEvent.class))).thenReturn(sampleEvent);

        SosEventResponse response = sosService.updateSosStatus(100L, updateReq, "admin@example.com");

        assertNotNull(response);
        assertEquals(SosStatus.RESOLVED, sampleEvent.getStatus());
        verify(sosEventRepository).save(sampleEvent);
    }

    @Test
    @DisplayName("Cannot transition SOS status back to ACTIVE")
    void testUpdateSosStatus_CannotTransitionToActive() {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.ACTIVE);
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));

        assertThrows(IllegalArgumentException.class, () ->
                sosService.updateSosStatus(100L, updateReq, "alice@example.com"));

        verify(sosEventRepository, never()).save(any(SosEvent.class));
    }

    @Test
    @DisplayName("Unauthorized user cannot modify another's SOS event")
    void testUpdateSosStatus_UnauthorizedUser_ThrowsAccessDenied() {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.RESOLVED);
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                sosService.updateSosStatus(100L, updateReq, "bob@example.com"));

        verify(sosEventRepository, never()).save(any(SosEvent.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when event ID does not exist")
    void testUpdateSosStatus_NotFound() {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.RESOLVED);
        when(sosEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                sosService.updateSosStatus(999L, updateReq, "alice@example.com"));
    }

    @Test
    @DisplayName("Should return active SOS event if one exists")
    void testGetActiveSos_ReturnsActiveEvent() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));
        when(sosEventRepository.findFirstByUserIdAndStatusOrderByTriggeredAtDesc(1L, SosStatus.ACTIVE))
                .thenReturn(Optional.of(sampleEvent));

        SosEventResponse response = sosService.getActiveSos("alice@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(SosStatus.ACTIVE, response.getStatus());
    }

    @Test
    @DisplayName("Should return null if no active SOS event exists")
    void testGetActiveSos_NoneActive_ReturnsNull() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));
        when(sosEventRepository.findFirstByUserIdAndStatusOrderByTriggeredAtDesc(1L, SosStatus.ACTIVE))
                .thenReturn(Optional.empty());

        SosEventResponse response = sosService.getActiveSos("alice@example.com");

        assertNull(response);
    }

    @Test
    @DisplayName("Should return full SOS event history in descending order")
    void testGetSosHistory_ReturnsList() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));
        when(sosEventRepository.findByUserIdOrderByTriggeredAtDesc(1L))
                .thenReturn(Collections.singletonList(sampleEvent));

        List<SosEventResponse> history = sosService.getSosHistory("alice@example.com");

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals(100L, history.get(0).getId());
    }

    @Test
    @DisplayName("Owner can view their own SOS event by ID")
    void testGetSosEventById_Success() {
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(regularUser));

        SosEventResponse response = sosService.getSosEventById(100L, "alice@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Admin can view any SOS event by ID")
    void testGetSosEventById_AdminCanAccess() {
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        SosEventResponse response = sosService.getSosEventById(100L, "admin@example.com");

        assertNotNull(response);
        assertEquals(100L, response.getId());
    }

    @Test
    @DisplayName("Unauthorized user cannot view another's SOS event")
    void testGetSosEventById_UnauthorizedUser_ThrowsAccessDenied() {
        when(sosEventRepository.findById(100L)).thenReturn(Optional.of(sampleEvent));
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                sosService.getSosEventById(100L, "bob@example.com"));
    }
}

