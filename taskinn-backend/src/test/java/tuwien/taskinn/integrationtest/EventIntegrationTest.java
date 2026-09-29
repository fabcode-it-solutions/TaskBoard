package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.multipart.MultipartFile;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.events.BookingStateChangedEvent;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.GuestRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.repo.RoomTypeRepository;
import tuwien.taskinn.services.BookingService;

@SpringBootTest
@RecordApplicationEvents
@DisplayName("Application Event Publishing Tests")
public class EventIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Autowired
    private GuestRepository guestRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private RoomTypeRepository roomTypeRepository;
    @Autowired
    private BookingRepository bookingRepository;

    @Test
    @DisplayName("checkInBooking should publish a BookingStateChangedEvent")
    void whenBookingIsCheckedIn_thenBookingStateChangedEventIsPublished() {
        Guest guest = guestRepository.save(Guest.builder()
                .firstName("Test")
                .lastName("Guest")
                .email("event@test.com")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build());
        RoomType rt = roomTypeRepository
                .save(RoomType.builder().name("Event Test").basePrice(BigDecimal.ONE).capacity(1).build());
        Room room = roomRepository.save(Room.builder()
                .roomNumber("E-101")
                .floor(1)
                .status(RoomStatus.AVAILABLE)
                .active(true)
                .roomType(rt)
                .svgPoints(List.of(new Point(0.0d, 0.0d), new Point(1.0d, 0.0d), new Point(1.0d, 1.0d),
                        new Point(0.0d, 1.0d)))
                .build());
        Booking booking = bookingRepository.save(Booking.builder()
                .guest(guest)
                .rooms(Set.of(room))
                .checkInDate(LocalDate.now())
                .checkOutDate(LocalDate.now().plusDays(3))
                .numAdults(1)
                .status(BookingStatus.CONFIRMED)
                .build());

        List<CheckInGuestDto> guests = createValidGuestDtos(room.getId());
        Map<String, MultipartFile> files = createValidFiles();

        bookingService.checkInBooking(booking.getId(), guests, files);

        long eventCount = applicationEvents.stream(BookingStateChangedEvent.class).count();
        assertThat(eventCount).isEqualTo(1);

        BookingStateChangedEvent event =
                applicationEvents.stream(BookingStateChangedEvent.class).findFirst().orElseThrow();
        assertThat(event.bookingId()).isEqualTo(booking.getId());
        assertThat(event.newStatus()).isEqualTo(BookingStatus.CHECKED_IN);

        System.out.println(event);
    }

    /**
     * Creates a list of valid CheckInGuestDto objects for testing purposes.
     *
     * @param roomId The room ID to assign the guest to.
     * @return A list containing one fully populated CheckInGuestDto object.
     */
    private List<CheckInGuestDto> createValidGuestDtos(Long roomId) {
        CheckInGuestDto guest1 = new CheckInGuestDto();
        guest1.setRoomId(roomId);
        guest1.setFirstName("John");
        guest1.setLastName("Doe");
        guest1.setDateOfBirth(LocalDate.of(1990, 5, 15));
        guest1.setEmail("john.doe.checkin@example.com");
        guest1.setPhoneNumber("+436601234567");

        List<CheckInGuestDto> guests = new ArrayList<>();
        guests.add(guest1);

        return guests;
    }

    /**
     * Creates a map of valid MultipartFile objects for testing purposes.
     *
     * @return A map containing one mock file for guest index 0.
     */
    private Map<String, MultipartFile> createValidFiles() {
        MockMultipartFile mockFile =
                new MockMultipartFile("guestFiles_0", "id-image.jpg", "image/jpeg", "test-image-content".getBytes());
        Map<String, MultipartFile> files = new HashMap<>();
        files.put("guestFiles_0", mockFile);
        return files;
    }
}
