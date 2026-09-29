package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import tuwien.taskinn.enums.PaymentStatus;

@Entity
@Table(name = "payments")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(unique = true)
    private String stripeChargeId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // --- Relationships ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Booking booking;
}
