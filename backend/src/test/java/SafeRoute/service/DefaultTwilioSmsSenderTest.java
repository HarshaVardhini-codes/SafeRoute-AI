package SafeRoute.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTwilioSmsSenderTest {

    @Test
    @DisplayName("Should throw IllegalStateException when account SID or auth token is empty")
    void testSendSms_MissingCredentials_ThrowsIllegalState() {
        DefaultTwilioSmsSender sender = new DefaultTwilioSmsSender("", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                sender.sendSms("+19998887777", "+15005550006", "Test Emergency Alert"));

        assertTrue(ex.getMessage().contains("credentials"));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when account SID is null")
    void testSendSms_NullCredentials_ThrowsIllegalState() {
        DefaultTwilioSmsSender sender = new DefaultTwilioSmsSender(null, null);

        assertThrows(IllegalStateException.class, () ->
                sender.sendSms("+19998887777", "+15005550006", "Test Emergency Alert"));
    }
}

