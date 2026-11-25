package SafeRoute.service;

import SafeRoute.dto.response.NotifiedContactSummary;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.SosEvent;

import java.util.List;

public interface EmergencyNotificationService {

    /**
     * Attempts to notify emergency contacts.
     * Returns the list of contact summaries and updates actual notified count.
     * If SMS/email integration is not configured, returns empty list with 0 count.
     */
    List<NotifiedContactSummary> notifyContacts(SosEvent event, List<EmergencyContact> contacts);
}

