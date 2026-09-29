package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.*;
import tuwien.taskinn.enums.MobileAuthTokenStatus;

@Entity
@Table(name = "mobile_auth_tokens",
        indexes = {@Index(name = "idx_mobile_auth_token", columnList = "token", unique = true),
                @Index(name = "idx_mobile_auth_session_id", columnList = "sessionId", unique = true)})
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileAuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employee employee;

    @Column(nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MobileAuthTokenStatus status;

    private Instant usedAt;
}
