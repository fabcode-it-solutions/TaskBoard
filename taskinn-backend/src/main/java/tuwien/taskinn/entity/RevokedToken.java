package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.time.Instant;

import lombok.*;

@Entity
@Table(name = "revoked_tokens", indexes = {@Index(name = "idx_revoked_jti", columnList = "jti", unique = true)})
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private Instant revokedAt = Instant.now();
}
