package SafeRoute.controller;

import SafeRoute.dto.request.EmergencyContactRequest;
import SafeRoute.dto.response.EmergencyContactResponse;
import SafeRoute.service.EmergencyContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    @PostMapping
    public ResponseEntity<EmergencyContactResponse> createContact(
            @Valid @RequestBody EmergencyContactRequest request,
            Authentication authentication) {

        EmergencyContactResponse response = emergencyContactService.createContact(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<EmergencyContactResponse>> getUserContacts(Authentication authentication) {
        List<EmergencyContactResponse> response = emergencyContactService.getUserContacts(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmergencyContactResponse> getContactById(
            @PathVariable Long id,
            Authentication authentication) {

        EmergencyContactResponse response = emergencyContactService.getContactById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmergencyContactResponse> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody EmergencyContactRequest request,
            Authentication authentication) {

        EmergencyContactResponse response = emergencyContactService.updateContact(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContact(
            @PathVariable Long id,
            Authentication authentication) {

        emergencyContactService.deleteContact(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

