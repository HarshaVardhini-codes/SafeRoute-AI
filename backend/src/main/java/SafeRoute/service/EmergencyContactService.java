package SafeRoute.service;

import SafeRoute.dto.request.EmergencyContactRequest;
import SafeRoute.dto.response.EmergencyContactResponse;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.User;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.repository.EmergencyContactRepository;
import SafeRoute.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;
    private final UserRepository userRepository;

    @Transactional
    public EmergencyContactResponse createContact(EmergencyContactRequest request, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);

        EmergencyContact contact = EmergencyContact.builder()
                .contactName(request.getContactName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .relationship(request.getRelationship().trim())
                .user(user)
                .build();

        EmergencyContact saved = emergencyContactRepository.save(contact);
        return mapToResponse(saved);
    }

    public List<EmergencyContactResponse> getUserContacts(String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);
        List<EmergencyContact> contacts = emergencyContactRepository.findByUserId(user.getId());
        return contacts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public EmergencyContactResponse getContactById(Long id, String currentUserEmail) {
        EmergencyContact contact = getContactEntityById(id);
        User currentUser = getUserByEmail(currentUserEmail);

        validateOwnership(contact, currentUser);

        return mapToResponse(contact);
    }

    @Transactional
    public EmergencyContactResponse updateContact(Long id, EmergencyContactRequest request, String currentUserEmail) {
        EmergencyContact contact = getContactEntityById(id);
        User currentUser = getUserByEmail(currentUserEmail);

        validateOwnership(contact, currentUser);

        contact.setContactName(request.getContactName().trim());
        contact.setPhoneNumber(request.getPhoneNumber().trim());
        contact.setRelationship(request.getRelationship().trim());

        EmergencyContact updated = emergencyContactRepository.save(contact);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteContact(Long id, String currentUserEmail) {
        EmergencyContact contact = getContactEntityById(id);
        User currentUser = getUserByEmail(currentUserEmail);

        validateOwnership(contact, currentUser);

        emergencyContactRepository.delete(contact);
    }

    private EmergencyContact getContactEntityById(Long id) {
        return emergencyContactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found with id: " + id));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private void validateOwnership(EmergencyContact contact, User user) {
        if (!contact.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have permission to access or modify this emergency contact");
        }
    }

    private EmergencyContactResponse mapToResponse(EmergencyContact contact) {
        return EmergencyContactResponse.builder()
                .id(contact.getId())
                .contactName(contact.getContactName())
                .phoneNumber(contact.getPhoneNumber())
                .relationship(contact.getRelationship())
                .userId(contact.getUser() != null ? contact.getUser().getId() : null)
                .userFullName(contact.getUser() != null ? contact.getUser().getFullName() : null)
                .build();
    }
}

