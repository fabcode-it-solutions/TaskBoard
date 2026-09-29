package tuwien.taskinn.datagenerator;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.repo.RoomTypeRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class RoomTypeDataGenerator {

    private final RoomTypeRepository roomTypeRepository;

    /**
     * Generates a predefined set of Room Types if none exist in the database.
     *
     * @return A list of all RoomType entities in the database (either pre-existing or newly created).
     */
    public List<RoomType> generateData() {
        if (roomTypeRepository.count() > 0) {
            log.info("Room types already exist. Skipping generation.");
            return roomTypeRepository.findAll();
        }

        log.info("Generating standard room type data...");

        List<RoomType> roomTypesToCreate = List.of(
                createRoomType("Single Room", "Compact room with one single bed, perfect for solo travelers", 1,
                        "80.00", 1, 0),
                createRoomType("Double Room", "Comfortable room with a queen size bed for couples", 2, "110.00", 0, 1),
                createRoomType("Deluxe Double", "Spacious double room with king bed and luxury amenities", 2, "140.00",
                        0, 1),
                createRoomType("Triple Room", "Room with one double bed and one single bed for three guests", 3,
                        "160.00", 1, 1),
                createRoomType("Family Suite", "Large suite with two double beds and living area", 4, "220.00", 0, 2),
                createRoomType("Executive Suite", "Luxury suite with separate bedroom, living room, and work area", 2,
                        "300.00", 0, 1),
                createRoomType("Superior Room", "Enhanced room with extra space and premium amenities", 2, "130.00", 0,
                        1),
                createRoomType("Junior Suite", "Suite with king bed and small sitting area", 2, "190.00", 0, 1));

        List<RoomType> savedRoomTypes = roomTypeRepository.saveAll(roomTypesToCreate);

        log.info("Room type data generation complete. Created {} new room types.", savedRoomTypes.size());

        return savedRoomTypes;
    }

    /**
     * A private helper method to build a RoomType object. This keeps the generation logic clean and readable.
     */
    private RoomType createRoomType(String name, String description, int capacity, String price, int singleBeds,
            int doubleBeds) {
        return RoomType.builder()
                .name(name)
                .description(description)
                .capacity(capacity)
                .basePrice(new BigDecimal(price))
                .singleBed(singleBeds)
                .doubleBed(doubleBeds)
                .build();
    }
}
