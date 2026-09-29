package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import tuwien.taskinn.enums.RoomStatus;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private Integer floor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection
    @CollectionTable(name = "room_svg_points", joinColumns = @JoinColumn(name = "room_id"))
    @OrderColumn(name = "point_order")
    @NotEmpty
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Point> svgPoints;

    // Override fields - null means inherit from RoomType
    private Integer capacityOverride;

    private BigDecimal basePriceOverride;

    private Integer singleBedOverride;

    private Integer doubleBedOverride;

    // --- Relationships ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RoomType roomType;

    @OneToMany(mappedBy = "room")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MaintenanceTicket> maintenanceTickets;

    public Integer getEffectiveCapacity() {
        return capacityOverride != null ? capacityOverride : roomType.getCapacity();
    }

    public BigDecimal getEffectiveBasePrice() {
        return basePriceOverride != null ? basePriceOverride : roomType.getBasePrice();
    }

    public Integer getEffectiveSingleBed() {
        return singleBedOverride != null ? singleBedOverride : roomType.getSingleBed();
    }

    public Integer getEffectiveDoubleBed() {
        return doubleBedOverride != null ? doubleBedOverride : roomType.getDoubleBed();
    }
}
