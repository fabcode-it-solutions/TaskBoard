package tuwien.taskinn.datagenerator;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.InventoryItem;
import tuwien.taskinn.entity.MinibarConsumption;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.repo.InventoryItemRepository;
import tuwien.taskinn.repo.MinibarConsumptionRepository;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class MinibarConsumptionDataGenerator {

    private final MinibarConsumptionRepository minibarConsumptionRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final Random random = new Random();

    private static final double BASE_CONSUMPTION_PROBABILITY = 0.3; // 30% chance a room has consumption on any given
                                                                    // day
    private static final double WEEKEND_MULTIPLIER = 2.5;
    private static final Set<String> POPULAR_ITEMS = Set.of("Coca Cola", "Evian Water 500ml", "Red Bull");

    /**
     * Generates minibar consumption data for past and current bookings.
     *
     * @param bookings A list of all generated bookings.
     */
    public void generateData(List<Booking> bookings, List<InventoryItem> allItems) {
        if (minibarConsumptionRepository.count() > 0) {
            log.info("Minibar consumption data already exists. Skipping generation.");
            return;
        }

        if (allItems.isEmpty()) {
            log.warn("No inventory items found. Skipping consumption data generation.");
            return;
        }

        log.info("Generating minibar consumption data for active and past bookings...");
        List<MinibarConsumption> consumptions = new ArrayList<>();

        List<Booking> eligibleBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN || b.getStatus() == BookingStatus.CHECKED_OUT)
                .toList();

        for (Booking booking : eligibleBookings) {
            for (LocalDate date = booking.getCheckInDate(); date.isBefore(booking.getCheckOutDate()); date =
                    date.plusDays(1)) {

                boolean isWeekend =
                        date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
                double consumptionChance = BASE_CONSUMPTION_PROBABILITY * (isWeekend ? WEEKEND_MULTIPLIER : 1.0);

                for (Room room : booking.getRooms()) {
                    if (random.nextDouble() < consumptionChance) {
                        int numberOfItemsConsumed = (random.nextDouble() < 0.8) ? 1 : 2;

                        for (int i = 0; i < numberOfItemsConsumed; i++) {
                            consumptions.add(generateRandomConsumption(date, booking, room, allItems));
                        }
                    }
                }
            }
        }

        minibarConsumptionRepository.saveAll(consumptions);
        log.info("Minibar consumption data generation complete. Created {} consumption records.", consumptions.size());
    }

    private MinibarConsumption generateRandomConsumption(LocalDate date, Booking booking, Room room,
            List<InventoryItem> allItems) {
        InventoryItem selectedItem = selectRandomItem(allItems);
        int quantity = (random.nextDouble() < 0.9) ? 1 : 2;
        LocalTime time = generateRandomTime();
        Instant recordedAt = LocalDateTime.of(date, time).toInstant(ZoneOffset.UTC);

        return MinibarConsumption.builder()
                .quantity(quantity)
                .recordedAt(recordedAt)
                .room(room)
                .booking(booking)
                .inventoryItem(selectedItem)
                .build();
    }

    private InventoryItem selectRandomItem(List<InventoryItem> allItems) {
        List<InventoryItem> popular = allItems.stream().filter(item -> POPULAR_ITEMS.contains(item.getName())).toList();
        List<InventoryItem> regular =
                allItems.stream().filter(item -> !POPULAR_ITEMS.contains(item.getName())).toList();

        if (random.nextDouble() < 0.7 && !popular.isEmpty()) {
            return popular.get(random.nextInt(popular.size()));
        } else if (!regular.isEmpty()) {
            return regular.get(random.nextInt(regular.size()));
        } else {
            return allItems.get(random.nextInt(allItems.size()));
        }
    }

    private LocalTime generateRandomTime() {
        int hour;
        if (random.nextDouble() < 0.6) {
            hour = 18 + random.nextInt(6);
        } else {
            hour = 9 + random.nextInt(9);
        }
        return LocalTime.of(hour, random.nextInt(60));
    }
}
