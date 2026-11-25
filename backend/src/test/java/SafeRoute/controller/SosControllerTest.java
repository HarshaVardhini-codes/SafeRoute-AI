package SafeRoute.controller;

import SafeRoute.dto.request.SosStatusUpdateRequest;
import SafeRoute.dto.request.SosTriggerRequest;
import SafeRoute.dto.response.SosEventResponse;
import SafeRoute.enums.SosStatus;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.GlobalExceptionHandler;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.service.SosService;
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

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SosControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SosService sosService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SosController sosController;

    private ObjectMapper objectMapper;
    private SosEventResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(sosController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = SosEventResponse.builder()
                .id(1L)
                .userId(10L)
                .userFullName("Jane Doe")
                .userPhoneNumber("9876543210")
                .latitude(12.9716)
                .longitude(77.5946)
                .message("Need help immediately!")
                .status(SosStatus.ACTIVE)
                .triggeredAt(LocalDateTime.now())
                .contactsNotifiedCount(0)
                .googleMapsUrl("https://maps.google.com/?q=12.971600,77.594600")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/sos/trigger should return 201 Created on valid input")
    void testTriggerSos_Success() throws Exception {
        SosTriggerRequest request = SosTriggerRequest.builder()
                .latitude(12.9716)
                .longitude(77.5946)
                .message("Need help immediately!")
                .build();

        when(sosService.triggerSos(any(SosTriggerRequest.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/sos/trigger")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.googleMapsUrl").exists());
    }

    @Test
    @DisplayName("POST /api/v1/sos/trigger should return 400 Bad Request on invalid coordinates")
    void testTriggerSos_InvalidCoordinates() throws Exception {
        SosTriggerRequest invalidRequest = SosTriggerRequest.builder()
                .latitude(120.0) // Invalid: > 90
                .longitude(77.5946)
                .build();

        mockMvc.perform(post("/api/v1/sos/trigger")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors.latitude").exists());
    }

    @Test
    @DisplayName("POST /api/v1/sos/trigger should return 400 Bad Request on duplicate active SOS")
    void testTriggerSos_DuplicateActive() throws Exception {
        SosTriggerRequest request = SosTriggerRequest.builder()
                .latitude(12.9716)
                .longitude(77.5946)
                .build();

        when(sosService.triggerSos(any(SosTriggerRequest.class), any()))
                .thenThrow(new IllegalStateException("An active SOS emergency is already in progress"));

        mockMvc.perform(post("/api/v1/sos/trigger")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An active SOS emergency is already in progress"));
    }

    @Test
    @DisplayName("PATCH /api/v1/sos/{id}/status should return 200 OK when authorized")
    void testUpdateSosStatus_Success() throws Exception {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.RESOLVED);
        sampleResponse.setStatus(SosStatus.RESOLVED);

        when(sosService.updateSosStatus(eq(1L), any(SosStatusUpdateRequest.class), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/sos/1/status")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/sos/{id}/status should return 403 Forbidden when unauthorized")
    void testUpdateSosStatus_Forbidden() throws Exception {
        SosStatusUpdateRequest updateReq = new SosStatusUpdateRequest(SosStatus.RESOLVED);

        when(sosService.updateSosStatus(eq(1L), any(SosStatusUpdateRequest.class), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to modify this SOS event"));

        mockMvc.perform(patch("/api/v1/sos/1/status")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /api/v1/sos/active should return 200 OK when active alert exists")
    void testGetActiveSos_Exists() throws Exception {
        when(sosService.getActiveSos(any())).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/sos/active")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/sos/active should return 204 No Content when no active alert")
    void testGetActiveSos_None() throws Exception {
        when(sosService.getActiveSos(any())).thenReturn(null);

        mockMvc.perform(get("/api/v1/sos/active")
                        .principal(authentication))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/sos/history should return 200 OK with list")
    void testGetSosHistory_Success() throws Exception {
        when(sosService.getSosHistory(any())).thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/v1/sos/history")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/sos/{id} should return 200 OK when found and authorized")
    void testGetSosEventById_Success() throws Exception {
        when(sosService.getSosEventById(eq(1L), any())).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/sos/1")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /api/v1/sos/{id} should return 404 Not Found when missing")
    void testGetSosEventById_NotFound() throws Exception {
        when(sosService.getSosEventById(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("SOS event not found with id: 99"));

        mockMvc.perform(get("/api/v1/sos/99")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/sos/{id} should return 403 Forbidden when unauthorized")
    void testGetSosEventById_Forbidden() throws Exception {
        when(sosService.getSosEventById(eq(1L), any()))
                .thenThrow(new AccessDeniedException("You do not have permission to view this SOS event"));

        mockMvc.perform(get("/api/v1/sos/1")
                        .principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}

