package tuwien.taskinn.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

import lombok.*;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false)
    private BigDecimal basePrice;

    private Integer singleBed;

    private Integer doubleBed;
}
