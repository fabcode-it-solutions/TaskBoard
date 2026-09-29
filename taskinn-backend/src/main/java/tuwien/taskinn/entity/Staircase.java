package tuwien.taskinn.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "staircases")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Staircase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private Double x;

    @Column(nullable = false)
    private Double y;

    @Column
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
