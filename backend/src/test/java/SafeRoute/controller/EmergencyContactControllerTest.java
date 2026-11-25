package SafeRoute.controller;

import SafeRoute.dto.request.EmergencyContactRequest;
import SafeRoute.dto.response.EmergencyContactResponse;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.GlobalExceptionHandler;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.service.EmergencyContactService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EmergencyContactControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmergencyContactService emergencyContactService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EmergencyContactController emergencyContactController;

    private ObjectMapper objectMapper;
    private EmergencyContactResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(emergencyContactController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = EmergencyContactResponse.builder()
                .id(1L)
                .contactName("Dad")
                .phoneNumber("9876543210")
                .relationship("Parent")
                .userId(10L)
                .userFullName("Alice")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/emergency-contacts should return 201 Created on valid input")
    void testCreateContact_Success() throws Exception {
        EmergencyContactRequest request = EmergencyContactRequest.builder()
                .contactName("Dad")
                .phoneNumber("9876543210")
                .relationship("Parent")
                .build();

        when(emergencyContactService.createContact(any(EmergencyContactRequest.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/emergency-contacts")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.contactName").value("Dad"))
                .andExpect(jsonPath("$.relationship").value("Parent"));
    }

    @Test
    @DisplayName("POST /api/v1/emergency-contacts should return 400 Bad Request on missing fields")
    void testCreateContact_MissingFields() throws Exception {
        EmergencyContactRequest invalidRequest = EmergencyContactRequest.builder()
                .contactName("")
                .phoneNumber("")
                .relationship("")
                .build();

        mockMvc.perform(post("/api/v1/emergency-contacts")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("GET /api/v1/emergency-contacts should return 200 OK with list of contacts")
    void testGetUserContacts_Success() throws Exception {
        when(emergencyContactService.getUserContacts(any()))
                .thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/v1/emergency-contacts")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contactName").value("Dad"))
                .andExpect(jsonPath("$[0].phoneNumber").value("9876543210"));
    }

    @Test
    @DisplayName("GET /api/v1/emergency-contacts/{id} should return 200 OK when found")
    void testGetContactById_Success() throws Exception {
        when(emergencyContactService.getContactById(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/emergency-contacts/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.contactName").value("Dad"));
    }

    @Test
    @DisplayName("GET /api/v1/emergency-contacts/{id} should return 404 Not Found when missing")
    void testGetContactById_NotFound() throws Exception {
        when(emergencyContactService.getContactById(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Emergency contact not found with id: 99"));

        mockMvc.perform(get("/api/v1/emergency-contacts/99")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Emergency contact not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/emergency-contacts/{id} should return 403 Forbidden when accessing another's contact")
    void testGetContactById_Forbidden() throws Exception {
        when(emergencyContactService.getContactById(eq(1L), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to access or modify this emergency contact"));

        mockMvc.perform(get("/api/v1/emergency-contacts/1")
                        .principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("PUT /api/v1/emergency-contacts/{id} should return 200 OK when updated")
    void testUpdateContact_Success() throws Exception {
        EmergencyContactRequest updateReq = EmergencyContactRequest.builder()
                .contactName("Father")
                .phoneNumber("9876543210")
                .relationship("Parent")
                .build();

        sampleResponse.setContactName("Father");
        when(emergencyContactService.updateContact(eq(1L), any(EmergencyContactRequest.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/v1/emergency-contacts/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("Father"));
    }

    @Test
    @DisplayName("PUT /api/v1/emergency-contacts/{id} should return 403 Forbidden when unauthorized")
    void testUpdateContact_Forbidden() throws Exception {
        EmergencyContactRequest updateReq = EmergencyContactRequest.builder()
                .contactName("Father")
                .phoneNumber("9876543210")
                .relationship("Parent")
                .build();

        when(emergencyContactService.updateContact(eq(1L), any(EmergencyContactRequest.class), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to access or modify this emergency contact"));

        mockMvc.perform(put("/api/v1/emergency-contacts/1")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("DELETE /api/v1/emergency-contacts/{id} should return 204 No Content when authorized")
    void testDeleteContact_Success() throws Exception {
        doNothing().when(emergencyContactService).deleteContact(eq(1L), any());

        mockMvc.perform(delete("/api/v1/emergency-contacts/1")
                        .principal(authentication))
                .andExpect(status().isNoContent());

        verify(emergencyContactService).deleteContact(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/emergency-contacts/{id} should return 403 Forbidden when unauthorized")
    void testDeleteContact_Forbidden() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to access or modify this emergency contact"))
                .when(emergencyContactService).deleteContact(eq(1L), any());

        mockMvc.perform(delete("/api/v1/emergency-contacts/1")
                        .principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}

