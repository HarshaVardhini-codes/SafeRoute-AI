package SafeRoute.dto.response;

import SafeRoute.enums.SosStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SosEventResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userPhoneNumber;
    private Double latitude;
    private Double longitude;
    private String message;
    private SosStatus status;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;
    private Integer contactsNotifiedCount;
    private String googleMapsUrl;
    private List<NotifiedContactSummary> notifiedContacts;
}

