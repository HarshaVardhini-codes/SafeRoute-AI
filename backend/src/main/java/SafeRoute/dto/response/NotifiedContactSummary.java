package SafeRoute.dto.response;

import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime dispatchedAt;
    private String errorMessage;
}
