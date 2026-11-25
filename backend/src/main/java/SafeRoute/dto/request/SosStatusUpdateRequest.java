package SafeRoute.dto.request;

import SafeRoute.enums.SosStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SosStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private SosStatus status;
}

