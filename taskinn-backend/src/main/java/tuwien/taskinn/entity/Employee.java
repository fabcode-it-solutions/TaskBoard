package tuwien.taskinn.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import tuwien.taskinn.enums.Roles;

@Entity
@Table(name = "employees")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @Column
    private BigDecimal hourlyWage;

    @Column
    private BigDecimal hourlyOvertimeWage;

    @Column
    private BigDecimal workingHoursPerWeek;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // --- Relationships ---
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "employee_roles", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "roles", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Roles> roles = new HashSet<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Shift> shifts;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PTO> ptos = new ArrayList<>();

    @OneToMany(mappedBy = "employee")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<CleaningTask> cleaningTasks;

    @OneToMany(mappedBy = "reporter")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MaintenanceTicket> maintenanceTickets;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduling_role_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SchedulingRole schedulingRole;

}
