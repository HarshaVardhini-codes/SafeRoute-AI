package SafeRoute.controller;

import SafeRoute.dto.request.SosStatusUpdateRequest;
import SafeRoute.dto.request.SosTriggerRequest;
import SafeRoute.dto.response.SosEventResponse;
import SafeRoute.service.SosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;

    @PostMapping("/trigger")
    public ResponseEntity<SosEventResponse> triggerSos(
            @Valid @RequestBody SosTriggerRequest request,
            Authentication authentication) {

        SosEventResponse response = sosService.triggerSos(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SosEventResponse> updateSosStatus(
            @PathVariable Long id,
            @Valid @RequestBody SosStatusUpdateRequest request,
            Authentication authentication) {

        SosEventResponse response = sosService.updateSosStatus(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<SosEventResponse> getActiveSos(Authentication authentication) {
        SosEventResponse response = sosService.getActiveSos(authentication.getName());
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<SosEventResponse>> getSosHistory(Authentication authentication) {
        List<SosEventResponse> history = sosService.getSosHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SosEventResponse> getSosEventById(
            @PathVariable Long id,
            Authentication authentication) {

        SosEventResponse response = sosService.getSosEventById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}

