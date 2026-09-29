package tuwien.taskinn.datagenerator;

import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.RoomRepository;

/**
 * Generates a realistic set of bookings (past, present, and future) to simulate a live hotel. This generator is the
 * core of the application's narrative and must run after guests and rooms are created.
 */
@Component
@Slf4j
@Profile("!test")
@RequiredArgsConstructor
public class BookingDataGenerator {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    private final Faker faker = new Faker(new Locale("en-US"));
    private final Random random = new Random();

    /**
     * Generates a narrative of bookings if the database is empty. The entire process is transactional, ensuring all or
     * no data is created.
     *
     * @param guests A list of pre-existing guests to create bookings for.
     * @param allRooms A list of all available rooms in the hotel.
     * @param count The total number of bookings to generate.
     * @return A list of the newly created Booking entities.
     */
    @Transactional
    public List<Booking> generateData(List<Guest> guests, List<Room> allRooms, int count) {
        if (bookingRepository.count() > 0) {
            log.info("Bookings already exist. Skipping generation.");
            return bookingRepository.findAll();
        }
        if (guests.isEmpty() || allRooms.isEmpty() || count <= 0) {
            log.warn("Cannot generate bookings without guests, rooms, or a positive count. Skipping.");
            return new ArrayList<>();
        }

        log.info("Generating a narrative of {} bookings...", count);
        LocalDate today = LocalDate.now();

        double pastRatio = 0.25, currentRatio = 0.035, arrivingTodayRatio = 0.02;

        int pastBookingsCount = (int) (count * pastRatio);
        int currentBookingsCount = (int) (count * currentRatio);
        int arrivingTodayCount = (int) (count * arrivingTodayRatio);
        int futureBookingsCount = count - pastBookingsCount - currentBookingsCount - arrivingTodayCount;

        if (currentBookingsCount + arrivingTodayCount > allRooms.size()) {
            int target = (int) (0.6 * allRooms.size());
            currentBookingsCount = target / 2;
            arrivingTodayCount = target - currentBookingsCount;
            futureBookingsCount = count - pastBookingsCount - currentBookingsCount - arrivingTodayCount;
        }

        for (int i = 0; i < pastBookingsCount; i++) {
            createAndSaveRandomBooking(guests, "past", today);
        }
        for (int i = 0; i < currentBookingsCount; i++) {
            createAndSaveRandomBooking(guests, "current", today);
        }
        for (int i = 0; i < arrivingTodayCount; i++) {
            createAndSaveRandomBooking(guests, "arriving", today);
        }
        for (int i = 0; i < futureBookingsCount; i++) {
            createAndSaveRandomBooking(guests, "future", today);
        }

        fastForwardCheckouts(today);

        log.info("Booking data generation complete. Created {} bookings.", bookingRepository.count());
        return bookingRepository.findAll();
    }

    /**
     * Creates AND SAVES a single randomized booking for a given time period. By saving immediately, subsequent
     * availability checks are accurate.
     */
    private void createAndSaveRandomBooking(List<Guest> guests, String period, LocalDate today) {
        LocalDate checkInDate, checkOutDate;
        switch (period) {
            case "past":
                checkOutDate = today.minusDays(faker.number().numberBetween(1, 150));
                checkInDate = checkOutDate.minusDays(faker.number().numberBetween(1, 7));
                break;
            case "current":
                checkInDate = today.minusDays(faker.number().numberBetween(1, 3));
                checkOutDate = today.plusDays(faker.number().numberBetween(2, 7));
                break;
            case "arriving":
                checkInDate = today;
                checkOutDate = today.plusDays(faker.number().numberBetween(1, 5));
                break;
            default: // future
                checkInDate = today.plusDays(faker.number().numberBetween(7, 180));
                checkOutDate = checkInDate.plusDays(faker.number().numberBetween(2, 7));
                break;
        }

        List<Room> availableRooms = roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate);
        if (availableRooms.isEmpty()) {
            log.warn("No available rooms found for dates {} to {}. Skipping booking creation.", checkInDate,
                    checkOutDate);
            return;
        }

        int numRoomsToBook = random.nextInt(2) + 1;
        if (numRoomsToBook > availableRooms.size()) {
            numRoomsToBook = availableRooms.size();
        }

        Collections.shuffle(availableRooms);
        Set<Room> selectedRooms = new HashSet<>(availableRooms.subList(0, numRoomsToBook));

        int totalCapacity = selectedRooms.stream().mapToInt(r -> r.getRoomType().getCapacity()).sum();
        int numAdults = faker.number().numberBetween(1, Math.max(2, totalCapacity));
        int numChildren =
                (numAdults < totalCapacity) ? faker.number().numberBetween(0, totalCapacity - numAdults + 1) : 0;

        if (numAdults + numChildren < numRoomsToBook) {
            int additionalAdults = numRoomsToBook - (numAdults + numChildren);
            numAdults += additionalAdults;
        }

        Guest guest = guests.get(random.nextInt(guests.size()));
        BookingStatus status;
        switch (period) {
            case "past":
                status = BookingStatus.CHECKED_OUT;
                break;
            case "current":
                status = BookingStatus.CHECKED_IN;
                break;
            default:
                status = BookingStatus.CONFIRMED;
                break;
        }

        boolean earlyCheckIn = random.nextDouble() < 0.15;
        boolean lateCheckOut = random.nextDouble() < 0.15;

        Booking booking = Booking.builder()
                .guest(guest)
                .rooms(selectedRooms)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .status(status)
                .earlyCheckIn(earlyCheckIn)
                .lateCheckOut(lateCheckOut)
                .numAdults(numAdults)
                .numChildren(numChildren)
                .createdManually(false)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        if (savedBooking.getStatus() == BookingStatus.CHECKED_IN) {
            savedBooking.getRooms().forEach(room -> room.setStatus(RoomStatus.OCCUPIED));
        } else if (savedBooking.getStatus() == BookingStatus.CHECKED_OUT
                && (savedBooking.getCheckOutDate().isEqual(today)
                        || savedBooking.getCheckOutDate().isEqual(today.minusDays(1)))) {
            savedBooking.getRooms().forEach(room -> room.setStatus(RoomStatus.DIRTY));
        }
        roomRepository.saveAll(savedBooking.getRooms());
    }

    private void addFinancialRecords(Booking booking) {
        long durationInDays = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        if (durationInDays <= 0)
            durationInDays = 1;

        BigDecimal totalPrice = booking.getRooms()
                .stream()
                .map(room -> room.getRoomType().getBasePrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(new BigDecimal(durationInDays));
    }

    private void fastForwardCheckouts(LocalDate today) {
        List<Booking> activeBookings = bookingRepository.findAll()
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                .filter(b -> !b.getCheckInDate().isAfter(today))
                .filter(b -> b.getCheckOutDate().isAfter(today))
                .limit(4)
                .toList();

        for (Booking booking : activeBookings) {
            booking.setStatus(BookingStatus.CHECKED_OUT);

            booking.getRooms().forEach(room -> room.setStatus(RoomStatus.DIRTY));
            booking.setCheckOutDate(today);

            roomRepository.saveAll(booking.getRooms());

            log.info("Fast-forwarded booking #{} to CHECKED_OUT status for demo.", booking.getId());
        }
    }
}
