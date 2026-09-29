package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.*;

@Entity
@Table(name = "password_reset_tokens",
        indexes = {@Index(name = "idx_pwd_reset_token", columnList = "token", unique = true)})
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Employee employee;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant usedAt;
}
