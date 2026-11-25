package SafeRoute.service;

public interface TwilioSmsSender {

    /**
     * Dispatches an SMS message via Twilio provider.
     *
     * @param toPhoneNumber recipient phone number
     * @param fromPhoneNumber Twilio sender phone number
     * @param messageBody text content of the SMS message
     * @return provider message SID or delivery reference
     */
    String sendSms(String toPhoneNumber, String fromPhoneNumber, String messageBody);
}

