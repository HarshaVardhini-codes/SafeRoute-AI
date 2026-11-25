package SafeRoute.service;

import SafeRoute.dto.request.EmergencyContactRequest;
import SafeRoute.dto.response.EmergencyContactResponse;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.User;
import SafeRoute.enums.Role;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.repository.EmergencyContactRepository;
import SafeRoute.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    @Mock
    private EmergencyContactRepository emergencyContactRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmergencyContactService emergencyContactService;

    private User userOne;
    private User userTwo;
    private EmergencyContact contactOne;
    private EmergencyContactRequest contactRequest;

    @BeforeEach
    void setUp() {
        userOne = User.builder()
                .id(1L)
                .fullName("User One")
                .email("user1@example.com")
                .role(Role.USER)
                .build();

        userTwo = User.builder()
                .id(2L)
                .fullName("User Two")
                .email("user2@example.com")
                .role(Role.USER)
                .build();

        contactOne = EmergencyContact.builder()
                .id(10L)
                .contactName("Mom")
                .phoneNumber("1234567890")
                .relationship("Parent")
                .user(userOne)
                .build();

        contactRequest = EmergencyContactRequest.builder()
                .contactName("Mom")
                .phoneNumber("1234567890")
                .relationship("Parent")
                .build();
    }

    @Test
    @DisplayName("Should create emergency contact linked to authenticated user")
    void testCreateContact_Success() {
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(userOne));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(contactOne);

        EmergencyContactResponse response = emergencyContactService.createContact(contactRequest, "user1@example.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Mom", response.getContactName());
        assertEquals(1L, response.getUserId());
        assertEquals("User One", response.getUserFullName());
        verify(emergencyContactRepository).save(any(EmergencyContact.class));
    }

    @Test
    @DisplayName("Should fetch all contacts belonging to the authenticated user")
    void testGetUserContacts_Success() {
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(userOne));
        when(emergencyContactRepository.findByUserId(1L)).thenReturn(Collections.singletonList(contactOne));

        List<EmergencyContactResponse> list = emergencyContactService.getUserContacts("user1@example.com");

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Mom", list.get(0).getContactName());
    }

    @Test
    @DisplayName("Should fetch contact by ID when owned by user")
    void testGetContactById_Success() {
        when(emergencyContactRepository.findById(10L)).thenReturn(Optional.of(contactOne));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(userOne));

        EmergencyContactResponse response = emergencyContactService.getContactById(10L, "user1@example.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Mom", response.getContactName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when contact ID does not exist")
    void testGetContactById_NotFound() {
        when(emergencyContactRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                emergencyContactService.getContactById(999L, "user1@example.com"));
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when accessing another user's contact")
    void testGetContactById_AccessDenied() {
        when(emergencyContactRepository.findById(10L)).thenReturn(Optional.of(contactOne));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(userTwo));

        assertThrows(AccessDeniedException.class, () ->
                emergencyContactService.getContactById(10L, "user2@example.com"));
    }

    @Test
    @DisplayName("Should update contact when owned by user")
    void testUpdateContact_Success() {
        EmergencyContactRequest updateReq = EmergencyContactRequest.builder()
                .contactName("Mother")
                .phoneNumber("9999999999")
                .relationship("Parent")
                .build();

        when(emergencyContactRepository.findById(10L)).thenReturn(Optional.of(contactOne));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(userOne));
        when(emergencyContactRepository.save(any(EmergencyContact.class))).thenReturn(contactOne);

        EmergencyContactResponse response = emergencyContactService.updateContact(10L, updateReq, "user1@example.com");

        assertNotNull(response);
        verify(emergencyContactRepository).save(contactOne);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when updating another user's contact")
    void testUpdateContact_AccessDenied() {
        when(emergencyContactRepository.findById(10L)).thenReturn(Optional.of(contactOne));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(userTwo));

        assertThrows(AccessDeniedException.class, () ->
                emergencyContactService.updateContact(10L, contactRequest, "user2@example.com"));

        verify(emergencyContactRepository, never()).save(any(EmergencyContact.class));
    }

    @Test
    @DisplayName("Should delete contact when owned by user")
    void testDeleteContact_Success() {
        when(emergencyContactRepository.findById(10L)).thenReturn(Optional.of(contactOne));
        when(userRepository.findByEmail("user1@example.com")).thenReturn(Optional.of(userOne));

        emergencyContactService.deleteContact(10L, "user1@example.com");

        verify(emergencyContactRepository).delete(contactOne);
    }

    @Test
    @DisplayName("Should throw AccessDeniedException when deleting another user's contact")
    void testDeleteContact_AccessDenied() {
        when(emergencyContactRepository.findById(10L)).thenReturn(Optional.of(contactOne));
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(userTwo));

        assertThrows(AccessDeniedException.class, () ->
                emergencyContactService.deleteContact(10L, "user2@example.com"));

        verify(emergencyContactRepository, never()).delete(any(EmergencyContact.class));
    }
}

