package SafeRoute.entity;

import SafeRoute.enums.SosStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sos_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SosEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Size(max = 500, message = "Message must not exceed 500 characters")
    @Column(name = "message", length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SosStatus status = SosStatus.ACTIVE;

    @Column(name = "contacts_notified_count", nullable = false)
    @Builder.Default
    private Integer contactsNotifiedCount = 0;

    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        if (this.triggeredAt == null) {
            this.triggeredAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = SosStatus.ACTIVE;
        }
        if (this.contactsNotifiedCount == null) {
            this.contactsNotifiedCount = 0;
        }
    }
}

