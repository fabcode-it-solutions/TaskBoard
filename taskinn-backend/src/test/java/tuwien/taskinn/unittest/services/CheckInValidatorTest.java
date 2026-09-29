package tuwien.taskinn.unittest.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityNotFoundException;
import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.validator.CheckInValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckInValidator Unit Tests")
public class CheckInValidatorTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private RoomRepository roomRepository;
    @InjectMocks
    private CheckInValidator validator;

    // ========================================================================================
    // validateImage Tests
    // ========================================================================================
    @Nested
    @DisplayName("validateImage Tests")
    class ValidateImageTests {

        @Test
        @DisplayName("should throw BusinessValidationException for images exceeding 10MB")
        void givenLargeImage_whenValidateImage_thenThrowsBusinessValidationException() {
            String base64 = "someBase64Content";
            byte[] decodedImage = new byte[11 * 1024 * 1024]; // 11MB
            int guestIndex = 0;

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateImage(base64, decodedImage, guestIndex));

            assertAll(() -> assertEquals("idImage", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Guest 1")),
                    () -> assertTrue(ex.getMessage().contains("10MB")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when image is null")
        void givenNullImage_whenValidateImage_thenThrowsBusinessValidationException() {
            String base64 = null;
            byte[] decodedImage = new byte[0];
            int guestIndex = 2;

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateImage(base64, decodedImage, guestIndex));

            assertAll(() -> assertEquals("idImage", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Guest 3")),
                    () -> assertTrue(ex.getMessage().contains("required")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when image is empty string")
        void givenEmptyImage_whenValidateImage_thenThrowsBusinessValidationException() {
            String base64 = "";
            byte[] decodedImage = new byte[0];
            int guestIndex = 0;

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateImage(base64, decodedImage, guestIndex));

            assertAll(() -> assertEquals("idImage", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("required")));
        }

        @Test
        @DisplayName("should pass for valid image under 10MB")
        void givenValidImage_whenValidateImage_thenNoExceptionThrown() {
            String base64 = "validBase64Content";
            byte[] decodedImage = new byte[5 * 1024 * 1024]; // 5MB
            int guestIndex = 0;

            assertDoesNotThrow(() -> validator.validateImage(base64, decodedImage, guestIndex));
        }
    }

    // ========================================================================================
    // validateGuest Tests
    // ========================================================================================
    @Nested
    @DisplayName("validateGuest Tests")
    class ValidateGuestTests {

        @Test
        @DisplayName("should throw BusinessValidationException when first name is missing")
        void givenMissingFirstName_whenValidateGuest_thenThrowsBusinessValidationException() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("");
            guest.setLastName("User");
            guest.setDateOfBirth(LocalDate.of(2000, 1, 1));

            BusinessValidationException ex =
                    assertThrows(BusinessValidationException.class, () -> validator.validateGuest(guest, 0));

            assertAll(() -> assertEquals("firstName", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Guest 1")),
                    () -> assertTrue(ex.getMessage().contains("First name")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when last name is missing")
        void givenMissingLastName_whenValidateGuest_thenThrowsBusinessValidationException() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("Test");
            guest.setLastName(null);
            guest.setDateOfBirth(LocalDate.of(2000, 1, 1));

            BusinessValidationException ex =
                    assertThrows(BusinessValidationException.class, () -> validator.validateGuest(guest, 1));

            assertAll(() -> assertEquals("lastName", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Guest 2")),
                    () -> assertTrue(ex.getMessage().contains("Last name")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when date of birth is null")
        void givenMissingDateOfBirth_whenValidateGuest_thenThrowsBusinessValidationException() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("Test");
            guest.setLastName("User");
            guest.setDateOfBirth(null);

            BusinessValidationException ex =
                    assertThrows(BusinessValidationException.class, () -> validator.validateGuest(guest, 0));

            assertAll(() -> assertEquals("dateOfBirth", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Date of birth")),
                    () -> assertTrue(ex.getMessage().contains("required")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when date of birth is in the future")
        void givenFutureDateOfBirth_whenValidateGuest_thenThrowsBusinessValidationException() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("Test");
            guest.setLastName("User");
            guest.setDateOfBirth(LocalDate.now().plusDays(1));

            BusinessValidationException ex =
                    assertThrows(BusinessValidationException.class, () -> validator.validateGuest(guest, 0));

            assertAll(() -> assertEquals("dateOfBirth", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("future")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when email format is invalid")
        void givenInvalidEmailFormat_whenValidateGuest_thenThrowsBusinessValidationException() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("Test");
            guest.setLastName("User");
            guest.setDateOfBirth(LocalDate.of(2000, 1, 1));
            guest.setEmail("invalid-email");

            BusinessValidationException ex =
                    assertThrows(BusinessValidationException.class, () -> validator.validateGuest(guest, 0));

            assertAll(() -> assertEquals("email", ex.getField()), () -> assertTrue(ex.getMessage().contains("Email")),
                    () -> assertTrue(ex.getMessage().contains("invalid")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when phone format is invalid")
        void givenInvalidPhoneFormat_whenValidateGuest_thenThrowsBusinessValidationException() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("Test");
            guest.setLastName("User");
            guest.setDateOfBirth(LocalDate.of(2000, 1, 1));
            guest.setPhoneNumber("12");

            BusinessValidationException ex =
                    assertThrows(BusinessValidationException.class, () -> validator.validateGuest(guest, 0));

            assertAll(() -> assertEquals("phoneNumber", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Phone")),
                    () -> assertTrue(ex.getMessage().contains("invalid")));
        }

        @Test
        @DisplayName("should pass when all guest fields are valid")
        void givenValidGuest_whenValidateGuest_thenNoExceptionThrown() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("John");
            guest.setLastName("Doe");
            guest.setDateOfBirth(LocalDate.of(1990, 5, 15));
            guest.setEmail("john.doe@example.com");
            guest.setPhoneNumber("+1234567890");

            assertDoesNotThrow(() -> validator.validateGuest(guest, 0));
        }

        @Test
        @DisplayName("should pass when email and phone are blank (optional for non-primary guests)")
        void givenBlankOptionalFields_whenValidateGuest_thenNoExceptionThrown() {
            CheckInGuestDto guest = new CheckInGuestDto();
            guest.setFirstName("Jane");
            guest.setLastName("Doe");
            guest.setDateOfBirth(LocalDate.of(1995, 3, 20));
            guest.setEmail("");
            guest.setPhoneNumber("");

            assertDoesNotThrow(() -> validator.validateGuest(guest, 1));
        }
    }

    // ========================================================================================
    // validateBookingStatusForCheckin Tests
    // ========================================================================================
    @Nested
    @DisplayName("validateBookingStatusForCheckin Tests")
    class ValidateBookingStatusTests {

        @Test
        @DisplayName("should throw EntityNotFoundException when booking does not exist")
        void givenMissingBooking_whenValidateBookingStatus_thenThrowsEntityNotFoundException() {
            Long bookingId = 42L;
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> validator.validateBookingStatusForCheckin(bookingId));

            assertTrue(ex.getMessage().contains("Booking not found"));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when check-in date is not today")
        void givenWrongCheckInDate_whenValidateBookingStatus_thenThrowsBusinessValidationException() {
            Long bookingId = 1L;
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now().plusDays(1))
                    .status(BookingStatus.CONFIRMED)
                    .build();
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateBookingStatusForCheckin(bookingId));

            assertAll(() -> assertEquals("booking", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("check-in date")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when booking status is not CONFIRMED")
        void givenNonConfirmedBooking_whenValidateBookingStatus_thenThrowsBusinessValidationException() {
            Long bookingId = 1L;
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CHECKED_IN)
                    .build();
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateBookingStatusForCheckin(bookingId));

            assertAll(() -> assertEquals("booking", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("CONFIRMED")));
        }
    }

    // ========================================================================================
    // validateCheckin Tests
    // ========================================================================================
    @Nested
    @DisplayName("validateCheckin Tests")
    class ValidateCheckinTests {

        @Test
        @DisplayName("should throw EntityNotFoundException when booking does not exist")
        void givenMissingBooking_whenValidateCheckin_thenThrowsEntityNotFound() {
            Long bookingId = 42L;
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

            List<CheckInGuestDto> guests = Collections.emptyList();
            Map<String, MultipartFile> files = Collections.emptyMap();

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));
            assertTrue(ex.getMessage().contains("Booking not found"));
        }

        @Test
        @DisplayName("should throw EntityNotFoundException when room does not exist")
        void givenMissingRoom_whenValidateCheckin_thenThrowsEntityNotFound() {
            Long bookingId = 13L;
            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(1)
                    .rooms(new HashSet<>())
                    .build();
            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(roomRepository.findById(101L)).thenReturn(Optional.empty());

            CheckInGuestDto guest = createValidGuest(101L);
            List<CheckInGuestDto> guests = List.of(guest);
            Map<String, MultipartFile> files = Collections.emptyMap();

            EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));
            assertTrue(ex.getMessage().contains("Room with id 101"));
        }

        @Test
        @DisplayName("should pass when all rooms have guests and all validations succeed")
        void givenValidRoomAssignments_whenValidateCheckin_thenSucceeds() {
            Long bookingId = 7L;

            RoomType doubleRoomType =
                    RoomType.builder().capacity(2).basePrice(BigDecimal.valueOf(200)).name("Double Room").build();
            RoomType singleRoomType =
                    RoomType.builder().capacity(1).basePrice(BigDecimal.valueOf(100)).name("Single Room").build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(doubleRoomType).active(true).build();
            Room room102 = Room.builder().id(102L).roomNumber("102").roomType(singleRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(2)
                    .rooms(new HashSet<>(List.of(room101, room102)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(roomRepository.findById(101L)).thenReturn(Optional.of(room101));
            when(roomRepository.findById(102L)).thenReturn(Optional.of(room102));

            CheckInGuestDto guestA = createValidGuest(101L);
            guestA.setEmail("guest.a@example.com");

            CheckInGuestDto guestB = createValidGuest(102L);
            guestB.setEmail("guest.b@example.com");
            guestB.setPhoneNumber("+9876543210");

            List<CheckInGuestDto> guests = List.of(guestA, guestB);
            Map<String, MultipartFile> files = Collections.emptyMap();

            assertDoesNotThrow(() -> validator.validateCheckin(bookingId, guests, files));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when too many guests assigned to one room")
        void givenTooManyGuestsForRoom_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 100L;

            RoomType singleRoomType =
                    RoomType.builder().capacity(1).name("Single Room").basePrice(BigDecimal.valueOf(100)).build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(singleRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(3)
                    .rooms(new HashSet<>(List.of(room101)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(roomRepository.findById(101L)).thenReturn(Optional.of(room101));

            CheckInGuestDto guestA = createValidGuest(101L);
            guestA.setEmail("a@example.com");

            CheckInGuestDto guestB = createValidGuest(101L);
            guestB.setFirstName("B");
            guestB.setEmail("b@example.com");
            guestB.setPhoneNumber("+9876543210");

            List<CheckInGuestDto> guests = List.of(guestA, guestB);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("room", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("accommodate")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when no guest has phone number")
        void givenNoGuestWithPhone_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 202L;

            RoomType singleRoomType =
                    RoomType.builder().capacity(1).name("Single Room").basePrice(BigDecimal.valueOf(100)).build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(singleRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(1)
                    .rooms(new HashSet<>(List.of(room101)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            // Note: roomRepository.findById not stubbed as validation fails before room lookup

            CheckInGuestDto guest = createValidGuest(101L);
            guest.setPhoneNumber(""); // No phone

            List<CheckInGuestDto> guests = List.of(guest);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("phoneNumber", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("phone")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when no guest has email")
        void givenNoGuestWithEmail_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 201L;

            RoomType singleRoomType =
                    RoomType.builder().capacity(1).name("Single Room").basePrice(BigDecimal.valueOf(100)).build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(singleRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(1)
                    .rooms(new HashSet<>(List.of(room101)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            // Note: roomRepository.findById not stubbed as validation fails before room lookup

            CheckInGuestDto guest = createValidGuest(101L);
            guest.setEmail(""); // No email

            List<CheckInGuestDto> guests = List.of(guest);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("email", ex.getField()), () -> assertTrue(ex.getMessage().contains("email")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when duplicate emails are provided")
        void givenDuplicateEmails_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 300L;

            RoomType doubleRoomType =
                    RoomType.builder().capacity(2).name("Double Room").basePrice(BigDecimal.valueOf(150)).build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(doubleRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(2)
                    .rooms(new HashSet<>(List.of(room101)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            // Note: roomRepository.findById not stubbed as validation fails before room lookup

            CheckInGuestDto guestA = createValidGuest(101L);
            guestA.setEmail("same@example.com");

            CheckInGuestDto guestB = createValidGuest(101L);
            guestB.setFirstName("Other");
            guestB.setEmail("same@example.com"); // Duplicate!
            guestB.setPhoneNumber("+9876543210");

            List<CheckInGuestDto> guests = List.of(guestA, guestB);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("email", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("Duplicate")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when guest count exceeds booking capacity")
        void givenTooManyGuests_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 400L;

            RoomType familyRoomType =
                    RoomType.builder().capacity(4).name("Family Room").basePrice(BigDecimal.valueOf(300)).build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(familyRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(2)
                    .numChildren(0)
                    .rooms(new HashSet<>(List.of(room101)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

            CheckInGuestDto guestA = createValidGuest(101L);
            guestA.setEmail("a@example.com");

            CheckInGuestDto guestB = createValidGuest(101L);
            guestB.setFirstName("B");
            guestB.setEmail("b@example.com");

            CheckInGuestDto guestC = createValidGuest(101L);
            guestC.setFirstName("C");
            guestC.setEmail("c@example.com");

            List<CheckInGuestDto> guests = List.of(guestA, guestB, guestC);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("guests", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("exceeds")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when room is not part of booking")
        void givenRoomNotInBooking_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 500L;

            RoomType singleRoomType =
                    RoomType.builder().capacity(1).name("Single Room").basePrice(BigDecimal.valueOf(100)).build();

            Room room101 = Room.builder().id(101L).roomNumber("101").roomType(singleRoomType).active(true).build();

            Room room999 = Room.builder().id(999L).roomNumber("999").roomType(singleRoomType).active(true).build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(1)
                    .rooms(new HashSet<>(List.of(room101))) // Only room 101 is in the booking
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(roomRepository.findById(999L)).thenReturn(Optional.of(room999));

            CheckInGuestDto guest = createValidGuest(999L); // Trying to check into room 999

            List<CheckInGuestDto> guests = List.of(guest);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("room", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("not part of this booking")));
        }

        @Test
        @DisplayName("should throw BusinessValidationException when room is inactive")
        void givenInactiveRoom_whenValidateCheckin_thenThrowsBusinessValidationException() {
            Long bookingId = 600L;

            RoomType singleRoomType =
                    RoomType.builder().capacity(1).name("Single Room").basePrice(BigDecimal.valueOf(100)).build();

            Room room101 = Room.builder()
                    .id(101L)
                    .roomNumber("101")
                    .roomType(singleRoomType)
                    .active(false) // Inactive!
                    .build();

            Booking booking = Booking.builder()
                    .id(bookingId)
                    .checkInDate(LocalDate.now())
                    .status(BookingStatus.CONFIRMED)
                    .numAdults(1)
                    .rooms(new HashSet<>(List.of(room101)))
                    .build();

            when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
            when(roomRepository.findById(101L)).thenReturn(Optional.of(room101));

            CheckInGuestDto guest = createValidGuest(101L);

            List<CheckInGuestDto> guests = List.of(guest);
            Map<String, MultipartFile> files = Collections.emptyMap();

            BusinessValidationException ex = assertThrows(BusinessValidationException.class,
                    () -> validator.validateCheckin(bookingId, guests, files));

            assertAll(() -> assertEquals("room", ex.getField()),
                    () -> assertTrue(ex.getMessage().contains("inactive")));
        }
    }

    // ========================================================================================
    // Helper Methods
    // ========================================================================================

    private CheckInGuestDto createValidGuest(Long roomId) {
        CheckInGuestDto guest = new CheckInGuestDto();
        guest.setFirstName("Test");
        guest.setLastName("User");
        guest.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guest.setRoomId(roomId);
        guest.setEmail("test@example.com");
        guest.setPhoneNumber("+1234567890");
        return guest;
    }
}
