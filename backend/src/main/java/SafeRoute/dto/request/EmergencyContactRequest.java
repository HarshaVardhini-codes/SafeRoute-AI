package SafeRoute.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContactRequest {

    @NotBlank(message = "Contact name is required")
    @Size(max = 100, message = "Contact name must not exceed 100 characters")
    private String contactName;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @NotBlank(message = "Relationship is required")
    @Size(max = 50, message = "Relationship must not exceed 50 characters")
    private String relationship;
}

