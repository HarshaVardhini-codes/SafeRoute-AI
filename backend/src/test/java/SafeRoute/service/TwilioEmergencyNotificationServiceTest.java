package SafeRoute.service;

import SafeRoute.dto.response.NotifiedContactSummary;
import SafeRoute.entity.EmergencyContact;
import SafeRoute.entity.SosEvent;
import SafeRoute.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwilioEmergencyNotificationServiceTest {

    @Mock
    private TwilioSmsSender twilioSmsSender;

    private User sampleUser;
    private SosEvent sampleEvent;
    private EmergencyContact contactMom;
    private EmergencyContact contactDad;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Alice Smith")
                .email("alice@example.com")
                .phoneNumber("+1234567890")
                .build();

        sampleEvent = SosEvent.builder()
                .id(100L)
                .user(sampleUser)
                .latitude(12.9716)
                .longitude(77.5946)
                .message("Need immediate assistance!")
                .build();

        contactMom = EmergencyContact.builder()
                .id(10L)
                .contactName("Mom")
                .phoneNumber("+19998887777")
                .relationship("Parent")
                .user(sampleUser)
                .build();

        contactDad = EmergencyContact.builder()
                .id(11L)
                .contactName("Dad")
                .phoneNumber("+19998887778")
                .relationship("Parent")
                .user(sampleUser)
                .build();
    }

    @Test
    @DisplayName("Should return SKIPPED_TWILIO_DISABLED when twilio.enabled is false without calling SMS sender")
    void testNotifyContacts_WhenTwilioDisabled_ReturnsSkipped() {
        TwilioEmergencyNotificationService service =
                new TwilioEmergencyNotificationService(twilioSmsSender, false, "+15005550006");

        List<NotifiedContactSummary> summaries = service.notifyContacts(sampleEvent, Arrays.asList(contactMom, contactDad));

        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        NotifiedContactSummary first = summaries.get(0);
        assertEquals("Mom", first.getContactName());
        assertEquals("+19998887777", first.getPhoneNumber());
        assertEquals("SKIPPED_TWILIO_DISABLED", first.getNotificationStatus());
        assertNotNull(first.getDispatchedAt());
        assertTrue(first.getErrorMessage().contains("disabled"));

        NotifiedContactSummary second = summaries.get(1);
        assertEquals("Dad", second.getContactName());
        assertEquals("+19998887778", second.getPhoneNumber());
        assertEquals("SKIPPED_TWILIO_DISABLED", second.getNotificationStatus());

        verifyNoInteractions(twilioSmsSender);
    }

    @Test
    @DisplayName("Should dispatch SMS and return SENT status when twilio.enabled is true and delivery succeeds")
    void testNotifyContacts_WhenTwilioEnabled_Success() {
        TwilioEmergencyNotificationService service =
                new TwilioEmergencyNotificationService(twilioSmsSender, true, "+15005550006");

        when(twilioSmsSender.sendSms(eq("+19998887777"), eq("+15005550006"), anyString()))
                .thenReturn("SM1111111111");
        when(twilioSmsSender.sendSms(eq("+19998887778"), eq("+15005550006"), anyString()))
                .thenReturn("SM2222222222");

        List<NotifiedContactSummary> summaries = service.notifyContacts(sampleEvent, Arrays.asList(contactMom, contactDad));

        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        assertEquals("SENT", summaries.get(0).getNotificationStatus());
        assertNull(summaries.get(0).getErrorMessage());
        assertNotNull(summaries.get(0).getDispatchedAt());

        assertEquals("SENT", summaries.get(1).getNotificationStatus());
        assertNull(summaries.get(1).getErrorMessage());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(twilioSmsSender, times(2)).sendSms(anyString(), eq("+15005550006"), messageCaptor.capture());

        String sentBody = messageCaptor.getAllValues().get(0);
        assertTrue(sentBody.contains("Alice Smith"));
        assertTrue(sentBody.contains("https://maps.google.com/?q=12.971600,77.594600"));
        assertTrue(sentBody.contains("Need immediate assistance!"));
    }

    @Test
    @DisplayName("One contact delivery failure should not prevent dispatch to subsequent contacts")
    void testNotifyContacts_PartialFailure_Resilience() {
        TwilioEmergencyNotificationService service =
                new TwilioEmergencyNotificationService(twilioSmsSender, true, "+15005550006");

        // Contact Mom fails with exception
        when(twilioSmsSender.sendSms(eq("+19998887777"), eq("+15005550006"), anyString()))
                .thenThrow(new RuntimeException("Twilio provider timeout"));

        // Contact Dad succeeds
        when(twilioSmsSender.sendSms(eq("+19998887778"), eq("+15005550006"), anyString()))
                .thenReturn("SM2222222222");

        List<NotifiedContactSummary> summaries = service.notifyContacts(sampleEvent, Arrays.asList(contactMom, contactDad));

        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        NotifiedContactSummary momSummary = summaries.get(0);
        assertEquals("Mom", momSummary.getContactName());
        assertEquals("FAILED", momSummary.getNotificationStatus());
        assertTrue(momSummary.getErrorMessage().contains("Twilio provider timeout"));

        NotifiedContactSummary dadSummary = summaries.get(1);
        assertEquals("Dad", dadSummary.getContactName());
        assertEquals("SENT", dadSummary.getNotificationStatus());
        assertNull(dadSummary.getErrorMessage());

        verify(twilioSmsSender).sendSms(eq("+19998887777"), eq("+15005550006"), anyString());
        verify(twilioSmsSender).sendSms(eq("+19998887778"), eq("+15005550006"), anyString());
    }

    @Test
    @DisplayName("Should return empty list without calling sender when contacts list is empty or null")
    void testNotifyContacts_EmptyOrNullContacts() {
        TwilioEmergencyNotificationService service =
                new TwilioEmergencyNotificationService(twilioSmsSender, true, "+15005550006");

        List<NotifiedContactSummary> emptyResult = service.notifyContacts(sampleEvent, Collections.emptyList());
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        List<NotifiedContactSummary> nullResult = service.notifyContacts(sampleEvent, null);
        assertNotNull(nullResult);
        assertTrue(nullResult.isEmpty());

        verifyNoInteractions(twilioSmsSender);
    }

    @Test
    @DisplayName("Should return FAILED status when twilio.phone-number is not configured")
    void testNotifyContacts_WhenPhoneNumberNotConfigured_ReturnsFailed() {
        TwilioEmergencyNotificationService service =
                new TwilioEmergencyNotificationService(twilioSmsSender, true, "");

        List<NotifiedContactSummary> summaries = service.notifyContacts(sampleEvent, Collections.singletonList(contactMom));

        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        assertEquals("FAILED", summaries.get(0).getNotificationStatus());
        assertTrue(summaries.get(0).getErrorMessage().contains("twilio.phone-number"));

        verifyNoInteractions(twilioSmsSender);
    }

    @Test
    @DisplayName("Should return FAILED status when contact phone number is null or empty")
    void testNotifyContacts_WhenContactPhoneNumberBlank_ReturnsFailed() {
        TwilioEmergencyNotificationService service =
                new TwilioEmergencyNotificationService(twilioSmsSender, true, "+15005550006");

        EmergencyContact invalidContact = EmergencyContact.builder()
                .id(99L)
                .contactName("Invalid Contact")
                .phoneNumber("   ")
                .relationship("Friend")
                .user(sampleUser)
                .build();

        List<NotifiedContactSummary> summaries = service.notifyContacts(sampleEvent, Collections.singletonList(invalidContact));

        assertNotNull(summaries);
        assertEquals(1, summaries.size());
        assertEquals("FAILED", summaries.get(0).getNotificationStatus());
        assertTrue(summaries.get(0).getErrorMessage().contains("phone number is missing"));

        verifyNoInteractions(twilioSmsSender);
    }
}

