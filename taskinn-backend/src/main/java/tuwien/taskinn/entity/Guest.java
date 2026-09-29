package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet; // <-- IMPORT ADDED
import java.util.Set;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "guests")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(unique = true)
    private String email;

    private String phone;

    private String idImageUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // --- Relationships ---
    @OneToMany(mappedBy = "guest")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Booking> bookings;

    /**
     * The set of all bookings this guest has been checked into as part of a group. This is the inverse side of the
     * ManyToMany relationship in the Booking entity.
     */
    @ManyToMany(mappedBy = "checkedInGuests", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private Set<Booking> associatedBookings = new HashSet<>();
}
