package tuwien.taskinn.datagenerator;

import com.github.javafaker.Faker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.MaintenanceTicket;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.Priority;
import tuwien.taskinn.enums.TicketStatus;
import tuwien.taskinn.repo.MaintenanceTicketRepository;

/**
 * Generates sample Maintenance Tickets on application startup if the database is empty. This generator is designed to
 * be called by a master data generator.
 */
@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class MaintenanceTicketDataGenerator {

    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final Faker faker = new Faker(new Locale("en-US"));
    private final Random random = new Random();

    /**
     * Generates a specified number of maintenance tickets with varied statuses and priorities. This method is
     * idempotent and will not run if tickets already exist.
     *
     * @param count The total number of tickets to generate.
     * @param rooms A list of available rooms to assign tickets to.
     * @param employees A list of available employees to assign as reporters.
     */
    public void generateData(int count, List<Room> rooms, List<Employee> employees) {
        if (maintenanceTicketRepository.count() > 0) {
            log.info("Maintenance tickets already exist. Skipping generation.");
            return;
        }
        if (rooms.isEmpty() || employees.isEmpty() || count <= 0) {
            log.warn("Cannot generate maintenance tickets with no rooms, employees, or zero count. Skipping.");
            return;
        }

        List<Room> activeRooms = rooms.stream().filter(Room::isActive).toList();

        log.info("Generating {} new sample maintenance tickets...", count);
        List<MaintenanceTicket> ticketsToCreate = new ArrayList<>();

        int openCount = (int) Math.max(1, count * 0.4);
        int inProgressCount = (int) Math.max(1, count * 0.3);

        for (int i = 0; i < openCount; i++) {
            ticketsToCreate.add(createRandomTicket(TicketStatus.OPEN, activeRooms, employees));
        }
        for (int i = 0; i < inProgressCount; i++) {
            ticketsToCreate.add(createRandomTicket(TicketStatus.IN_PROGRESS, activeRooms, employees));
        }
        while (ticketsToCreate.size() < count) {
            ticketsToCreate.add(createRandomTicket(TicketStatus.RESOLVED, activeRooms, employees));
        }

        maintenanceTicketRepository.saveAll(ticketsToCreate);
        log.info("Maintenance ticket data generation complete. Created {} new tickets.", ticketsToCreate.size());
    }

    /**
     * A helper method to create a single, randomized MaintenanceTicket.
     */
    private MaintenanceTicket createRandomTicket(TicketStatus status, List<Room> activeRooms,
            List<Employee> employees) {
        String[] issueTypes = {"Plumbing", "Electrical", "Appliance", "Wi-Fi", "Furniture"};
        String issue = issueTypes[random.nextInt(issueTypes.length)];

        String title = "";
        String description = "";

        switch (issue) {
            case "Plumbing":
                title = "Leaky " + faker.options().option("Faucet", "Shower Head", "Toilet Base");
                description = "The " + title.toLowerCase() + " in the bathroom is dripping and needs repair.";
                break;
            case "Electrical":
                title = "Outlet Not Working";
                description = "The electrical outlet near the " + faker.options().option("desk", "bed", "TV")
                        + " is not providing power.";
                break;
            case "Appliance":
                title = faker.options().option("Minifridge", "Television", "A/C Unit") + " Issue";
                description = "The " + title.toLowerCase() + " is "
                        + faker.options().option("making a strange noise", "not turning on", "not cooling properly")
                        + ".";
                break;
            case "Wi-Fi":
                title = "Weak Wi-Fi Signal";
                description = "Guests have reported that the Wi-Fi connection is unstable and slow in this room.";
                break;
            case "Furniture":
                title = "Damaged " + faker.options().option("Chair Leg", "Dresser Drawer", "Bed Frame");
                description = "The " + title.toLowerCase()
                        + " is wobbly and appears to be broken. It may be a safety hazard.";
                break;
        }

        MaintenanceTicket.MaintenanceTicketBuilder ticketBuilder = MaintenanceTicket.builder()
                .title(title)
                .description(description)
                .priority(getRandomPriority())
                .status(status)
                .room(getRandomElement(activeRooms))
                .reporter(getRandomElement(employees));

        if (status == TicketStatus.RESOLVED || status == TicketStatus.IN_PROGRESS) {
            ticketBuilder.createdAt(Instant.now().minus(random.nextInt(1, 15), ChronoUnit.DAYS));
        }

        return ticketBuilder.build();
    }

    private Priority getRandomPriority() {
        int r = random.nextInt(10);
        if (r < 3) {
            return Priority.LOW;
        }
        if (r < 6) {
            return Priority.MEDIUM;
        }
        if (r < 8) {
            return Priority.HIGH;
        }
        return Priority.URGENT;
    }

    private <T> T getRandomElement(List<T> list) {
        if (list.isEmpty())
            return null;
        return list.get(random.nextInt(list.size()));
    }
}
