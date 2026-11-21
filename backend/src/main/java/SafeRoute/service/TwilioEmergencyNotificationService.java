package SafeRoute.service;

import SafeRoute.dto.response.NotifiedContactSummary;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.SosEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Primary
public class TwilioEmergencyNotificationService implements EmergencyNotificationService {

    private final TwilioSmsSender twilioSmsSender;
    private final boolean twilioEnabled;
    private final String twilioPhoneNumber;

    @Autowired
    public TwilioEmergencyNotificationService(
            TwilioSmsSender twilioSmsSender,
            @Value("${twilio.enabled:false}") boolean twilioEnabled,
            @Value("${twilio.phone-number:}") String twilioPhoneNumber) {
        this.twilioSmsSender = twilioSmsSender;
        this.twilioEnabled = twilioEnabled;
        this.twilioPhoneNumber = twilioPhoneNumber != null ? twilioPhoneNumber.trim() : "";
    }

    public TwilioEmergencyNotificationService(TwilioSmsSender twilioSmsSender) {
        this(twilioSmsSender, false, "");
    }

    @Override
    public List<NotifiedContactSummary> notifyContacts(SosEvent event, List<EmergencyContact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return Collections.emptyList();
        }

        // If Twilio SMS notifications are disabled in configuration
        if (!twilioEnabled) {
            log.info("Twilio SMS is disabled (twilio.enabled=false). Skipping real-time SMS dispatch for {} contacts.", contacts.size());
            return contacts.stream()
                    .map(contact -> NotifiedContactSummary.builder()
                            .contactName(contact.getContactName())
                            .phoneNumber(contact.getPhoneNumber())
                            .relationship(contact.getRelationship())
                            .notificationStatus("SKIPPED_TWILIO_DISABLED")
                            .dispatchedAt(LocalDateTime.now())
                            .errorMessage("Twilio SMS notifications are disabled in application configuration")
                            .build())
                    .toList();
        }

        // Validate sender phone number
        if (twilioPhoneNumber.isEmpty()) {
            log.error("Twilio SMS is enabled but twilio.phone-number is not configured. Failing notification for {} contacts.", contacts.size());
            return contacts.stream()
                    .map(contact -> NotifiedContactSummary.builder()
                            .contactName(contact.getContactName())
                            .phoneNumber(contact.getPhoneNumber())
                            .relationship(contact.getRelationship())
                            .notificationStatus("FAILED")
                            .dispatchedAt(LocalDateTime.now())
                            .errorMessage("Twilio sender phone number (twilio.phone-number) is not configured")
                            .build())
                    .toList();
        }

        // Construct standard emergency SMS message text
        String userName = (event != null && event.getUser() != null && event.getUser().getFullName() != null)
                ? event.getUser().getFullName()
                : "SafeRoute User";
        double lat = event != null ? event.getLatitude() : 0.0;
        double lon = event != null ? event.getLongitude() : 0.0;
        String mapUrl = String.format("https://maps.google.com/?q=%f,%f", lat, lon);
        String customMsg = (event != null && event.getMessage() != null && !event.getMessage().trim().isEmpty())
                ? "\nMessage: " + event.getMessage().trim()
                : "";
        String messageBody = String.format(
                "EMERGENCY ALERT: %s has triggered an SOS on SafeRoute AI!%s\nLocation: %s\nPlease assist or contact emergency services immediately.",
                userName, customMsg, mapUrl
        );

        List<NotifiedContactSummary> summaries = new ArrayList<>();

        for (EmergencyContact contact : contacts) {
            String destPhone = contact.getPhoneNumber();
            try {
                if (destPhone == null || destPhone.trim().isEmpty()) {
                    throw new IllegalArgumentException("Contact phone number is missing");
                }

                String sid = twilioSmsSender.sendSms(destPhone.trim(), twilioPhoneNumber, messageBody);
                log.info("SOS alert SMS successfully sent to contact '{}' at {} (SID: {})",
                        contact.getContactName(), destPhone, sid);

                summaries.add(NotifiedContactSummary.builder()
                        .contactName(contact.getContactName())
                        .phoneNumber(contact.getPhoneNumber())
                        .relationship(contact.getRelationship())
                        .notificationStatus("SENT")
                        .dispatchedAt(LocalDateTime.now())
                        .build());
            } catch (Exception ex) {
                log.error("Failed to send SOS SMS to contact '{}' at {}: {}",
                        contact.getContactName(), destPhone, ex.getMessage());

                summaries.add(NotifiedContactSummary.builder()
                        .contactName(contact.getContactName())
                        .phoneNumber(contact.getPhoneNumber())
                        .relationship(contact.getRelationship())
                        .notificationStatus("FAILED")
                        .dispatchedAt(LocalDateTime.now())
                        .errorMessage(ex.getMessage() != null ? ex.getMessage() : "Twilio delivery failure")
                        .build());
            }
        }

        return summaries;
    }
}
