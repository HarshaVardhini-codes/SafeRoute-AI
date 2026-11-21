package SafeRoute.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTwilioSmsSender implements TwilioSmsSender {

    private final String accountSid;
    private final String authToken;
    private volatile boolean initialized = false;

    public DefaultTwilioSmsSender(
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken) {
        this.accountSid = accountSid != null ? accountSid.trim() : "";
        this.authToken = authToken != null ? authToken.trim() : "";
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            if (accountSid.isEmpty() || authToken.isEmpty()) {
                throw new IllegalStateException("Twilio credentials (account-sid or auth-token) are missing or not configured");
            }
            Twilio.init(accountSid, authToken);
            initialized = true;
            log.info("Twilio SDK initialized successfully");
        }
    }

    @Override
    public String sendSms(String toPhoneNumber, String fromPhoneNumber, String messageBody) {
        ensureInitialized();
        Message message = Message.creator(
                new PhoneNumber(toPhoneNumber),
                new PhoneNumber(fromPhoneNumber),
                messageBody
        ).create();
        return message.getSid();
    }
}

