package tuwien.taskinn.datagenerator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tuwien.taskinn.entity.*;

import java.util.List;

@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class MainDataGenerator {

    @Value("${app.datagen.staff-count}")
    private int staffCount;

    @Value("${app.datagen.maintenance-ticket-count}")
    private int maintenanceTicketCount;

    @Value("${app.datagen.guest-count}")
    private int guestCount;

    @Value("${app.datagen.booking-count}")
    private int bookingCount;

    private final AdminAccountDataGenerator adminAccountDataGenerator;
    private final StaffDataGenerator staffDataGenerator;

    private final RoomTypeDataGenerator roomTypeDataGenerator;
    private final RoomDataGenerator roomDataGenerator;

    private final MaintenanceTicketDataGenerator maintenanceTicketDataGenerator;

    private final GuestDataGenerator guestDataGenerator;
    private final BookingDataGenerator bookingDataGenerator;

    private final UnavailabilityDataGenerator unavailabilityDataGenerator;

    private final HistoricalShiftDataGenerator historicalShiftDataGenerator;

    private final InventoryItemDataGenerator inventoryItemDataGenerator;

    private final MinibarConsumptionDataGenerator minibarConsumptionDataGenerator;

    private final HistoricalCleaningTaskDataGenerator historicalCleaningTaskDataGenerator;

    private final ShiftRolesTemplatesGenerator shiftRolesTemplatesGenerator;

    private final PaymentDataGenerator paymentDataGenerator;

    @PostConstruct
    public void generateData() {
        // Personal data generation
        Employee adminUser = adminAccountDataGenerator.generateData();
        List<Employee> employees = staffDataGenerator.generateData(staffCount);
        shiftRolesTemplatesGenerator.generate(employees);
        unavailabilityDataGenerator.generateData(employees);
        historicalShiftDataGenerator.generateData(employees);

        // Hotel structure data generation
        List<RoomType> roomTypes = roomTypeDataGenerator.generateData();
        List<Room> rooms = roomDataGenerator.generateData(roomTypes);
        maintenanceTicketDataGenerator.generateData(maintenanceTicketCount, rooms, employees);

        // Booking data generation
        List<Guest> guests = guestDataGenerator.generateData(guestCount);
        List<Booking> bookings = bookingDataGenerator.generateData(guests, rooms, bookingCount);
        List<InventoryItem> inventoryItems = inventoryItemDataGenerator.generateData();
        minibarConsumptionDataGenerator.generateData(bookings, inventoryItems);
        paymentDataGenerator.generateData();

        // Cleaning task data generation
        historicalCleaningTaskDataGenerator.generateData();
    }
}
