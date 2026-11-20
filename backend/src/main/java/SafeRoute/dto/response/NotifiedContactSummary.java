package SafeRoute.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifiedContactSummary {

    private String contactName;
    private String phoneNumber;
    private String relationship;
    private String notificationStatus;
}

