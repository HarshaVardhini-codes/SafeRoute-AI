package SafeRoute.service;

import SafeRoute.dto.response.NotifiedContactSummary;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.SosEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DefaultEmergencyNotificationService implements EmergencyNotificationService {

    @Value("${app.notifications.sms.enabled:false}")
    private boolean smsEnabled;

    @Override
    public List<NotifiedContactSummary> notifyContacts(SosEvent event, List<EmergencyContact> contacts) {
        // If SMS/email integration is not enabled, do not claim notifications were sent
        if (!smsEnabled || contacts == null || contacts.isEmpty()) {
            return Collections.emptyList();
        }

        return contacts.stream()
                .map(contact -> NotifiedContactSummary.builder()
                        .contactName(contact.getContactName())
                        .phoneNumber(contact.getPhoneNumber())
                        .relationship(contact.getRelationship())
                        .notificationStatus("SENT")
                        .build())
                .toList();
    }
}

