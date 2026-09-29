package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tuwien.taskinn.entity.Point;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.RoomStatus;

import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.validator.RoomValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomValidator Unit Tests")
public class RoomValidatorTest {

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private RoomValidator roomValidator;

    @Test
    @DisplayName("Should not throw exception when rooms are on different floors")
    void validateOverlap_whenRoomsOnDifferentFloors_doesNotThrow() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                new Point(0.0, 100.0));

        Room existingRoom = createRoom(1L, "101", 2, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.emptyList());

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateOverlap(null, floor, newSvgPoints));
    }

    @Test
    @DisplayName("Should not throw exception when rooms do not overlap")
    void validateOverlap_whenRoomsDoNotOverlap_doesNotThrow() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(150.0, 0.0), new Point(250.0, 0.0), new Point(250.0, 100.0),
                new Point(150.0, 100.0));

        Room existingRoom = createRoom(1L, "101", floor, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(existingRoom));

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateOverlap(null, floor, newSvgPoints));
    }

    @Test
    @DisplayName("Should throw exception when new room vertex is inside existing room")
    void validateOverlap_whenNewRoomVertexInsideExistingRoom_throwsException() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(50.0, 50.0), // This point is inside existing room
                new Point(150.0, 50.0), new Point(150.0, 150.0), new Point(50.0, 150.0));

        Room existingRoom = createRoom(1L, "101", floor, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(existingRoom));

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateOverlap(null, floor, newSvgPoints));

        assertAll(() -> assertThat(exception.getMessage()).contains("Overlap detected"),
                () -> assertThat(exception.getMessage()).contains("101"),
                () -> assertThat(exception.getMessage()).contains("floor " + floor));
    }

    @Test
    @DisplayName("Should throw exception when existing room vertex is inside new room")
    void validateOverlap_whenExistingRoomVertexInsideNewRoom_throwsException() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(0.0, 0.0), new Point(200.0, 0.0), new Point(200.0, 200.0),
                new Point(0.0, 200.0));

        Room existingRoom = createRoom(1L, "102", floor, Arrays.asList(new Point(50.0, 50.0), // This point will be
                                                                                              // inside new room
                new Point(150.0, 50.0), new Point(150.0, 150.0), new Point(50.0, 150.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(existingRoom));

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateOverlap(null, floor, newSvgPoints));

        assertAll(() -> assertThat(exception.getMessage()).contains("Overlap detected"),
                () -> assertThat(exception.getMessage()).contains("102"),
                () -> assertThat(exception.getMessage()).contains("floor " + floor));
    }

    @Test
    @DisplayName("Should throw exception when rooms completely overlap")
    void validateOverlap_whenRoomsCompletelyOverlap_throwsException() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                new Point(0.0, 100.0));

        Room existingRoom = createRoom(1L, "103", floor, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(existingRoom));

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateOverlap(null, floor, newSvgPoints));

        assertAll(() -> assertThat(exception.getMessage()).contains("Overlap detected"),
                () -> assertThat(exception.getMessage()).contains("103"),
                () -> assertThat(exception.getMessage()).contains("floor " + floor));
    }

    @Test
    @DisplayName("Should not throw exception when updating room with same ID (skips itself)")
    void validateOverlap_whenUpdatingRoomWithSameId_doesNotThrow() {
        // Given
        Long roomId = 1L;
        Integer floor = 1;
        List<Point> updatedSvgPoints = Arrays.asList(new Point(0.0, 0.0), new Point(120.0, 0.0),
                new Point(120.0, 120.0), new Point(0.0, 120.0));

        Room existingRoom = createRoom(roomId, "101", floor, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(existingRoom));

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateOverlap(roomId, floor, updatedSvgPoints));
    }

    @Test
    @DisplayName("Should throw exception when updating room overlaps with another room")
    void validateOverlap_whenUpdatingRoomOverlapsWithAnotherRoom_throwsException() {
        // Given
        Long roomIdBeingUpdated = 1L;
        Integer floor = 1;
        List<Point> updatedSvgPoints = Arrays.asList(new Point(50.0, 50.0), new Point(250.0, 50.0),
                new Point(250.0, 150.0), new Point(50.0, 150.0));

        Room roomBeingUpdated = createRoom(roomIdBeingUpdated, "101", floor, Arrays.asList(new Point(0.0, 0.0),
                new Point(100.0, 0.0), new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        Room otherRoom = createRoom(2L, "102", floor, Arrays.asList(new Point(200.0, 100.0), // This will overlap with
                                                                                             // updated room
                new Point(300.0, 100.0), new Point(300.0, 200.0), new Point(200.0, 200.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Arrays.asList(roomBeingUpdated, otherRoom));

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateOverlap(roomIdBeingUpdated, floor, updatedSvgPoints));

        assertAll(() -> assertThat(exception.getMessage()).contains("Overlap detected"),
                () -> assertThat(exception.getMessage()).contains("102"),
                () -> assertThat(exception.getMessage()).contains("floor " + floor));
    }

    @Test
    @DisplayName("Should not throw exception when rooms share an edge but do not overlap")
    void validateOverlap_whenRoomsShareEdge_doesNotThrow() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(100.0, 0.0), // Shares edge at x=100
                new Point(200.0, 0.0), new Point(200.0, 100.0), new Point(100.0, 100.0));

        Room existingRoom = createRoom(1L, "101", floor, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), // Edge
                                                                                                                   // at
                                                                                                                   // x=100
                new Point(100.0, 100.0), new Point(0.0, 100.0)), true);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(existingRoom));

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateOverlap(null, floor, newSvgPoints));
    }

    @Test
    @DisplayName("Should not throw exception when overlapping room is inactive")
    void validateOverlap_whenOverlappingRoomIsInactive_doesNotThrow() {
        // Given
        Integer floor = 1;
        List<Point> newSvgPoints = Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0), new Point(100.0, 100.0),
                new Point(0.0, 100.0));

        // Create an inactive room that would normally cause an overlap
        Room inactiveRoom = createRoom(1L, "101", floor, Arrays.asList(new Point(0.0, 0.0), new Point(100.0, 0.0),
                new Point(100.0, 100.0), new Point(0.0, 100.0)), false);

        when(roomRepository.findAllByFloor(floor)).thenReturn(Collections.singletonList(inactiveRoom));

        // When & Then - should not throw because inactive rooms are skipped
        assertDoesNotThrow(() -> roomValidator.validateOverlap(null, floor, newSvgPoints));
    }

    // ==================== validateRoomNumberUniqueness Tests ====================

    @Test
    @DisplayName("Should not throw exception when creating room with unique room number")
    void validateRoomNumberUniqueness_whenCreatingRoomWithUniqueNumber_doesNotThrow() {
        // Given
        String roomNumber = "101";

        when(roomRepository.existsActiveRoomByNumber(roomNumber, null)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberUniqueness(null, roomNumber));
    }

    @Test
    @DisplayName("Should throw exception when creating room with existing active room number")
    void validateRoomNumberUniqueness_whenCreatingRoomWithExistingNumber_throwsException() {
        // Given
        String roomNumber = "102";

        when(roomRepository.existsActiveRoomByNumber(roomNumber, null)).thenReturn(true);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberUniqueness(null, roomNumber));

        assertThat(exception.getMessage()).contains("Room number '" + roomNumber + "' is already in use");
    }

    @Test
    @DisplayName("Should not throw exception when updating room keeping same room number")
    void validateRoomNumberUniqueness_whenUpdatingRoomWithSameNumber_doesNotThrow() {
        // Given
        Long roomId = 1L;
        String roomNumber = "103";

        when(roomRepository.existsActiveRoomByNumber(roomNumber, roomId)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberUniqueness(roomId, roomNumber));
    }

    @Test
    @DisplayName("Should throw exception when updating room with another room's number")
    void validateRoomNumberUniqueness_whenUpdatingRoomWithExistingNumber_throwsException() {
        // Given
        Long roomId = 1L;
        String roomNumber = "104";

        when(roomRepository.existsActiveRoomByNumber(roomNumber, roomId)).thenReturn(true);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberUniqueness(roomId, roomNumber));

        assertThat(exception.getMessage()).contains("Room number '" + roomNumber + "' is already in use");
    }

    @Test
    @DisplayName("Should not throw exception when room number exists but room is inactive")
    void validateRoomNumberUniqueness_whenRoomNumberExistsButInactive_doesNotThrow() {
        // Given
        String roomNumber = "105";
        // Mock returns false because the existing room with this number is inactive
        when(roomRepository.existsActiveRoomByNumber(roomNumber, null)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberUniqueness(null, roomNumber));
    }

    /**
     * Helper method to create a Room entity for testing.
     */
    private Room createRoom(Long id, String roomNumber, Integer floor, List<Point> svgPoints, Boolean active) {
        RoomType roomType = RoomType.builder().id(1L).name("Standard").build();

        return Room.builder()
                .id(id)
                .roomNumber(roomNumber)
                .floor(floor)
                .status(RoomStatus.AVAILABLE)
                .svgPoints(svgPoints)
                .roomType(roomType)
                .active(active)
                .build();
    }

    // ==================== validateRoomNumberFloorPrefix Tests ====================

    @Test
    @DisplayName("Should not throw exception when room number has correct floor prefix")
    void validateRoomNumberFloorPrefix_whenValidRoomNumber_doesNotThrow() {
        // Given
        String roomNumber = "101";
        Integer floor = 1;

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));
    }

    @Test
    @DisplayName("Should not throw exception for floor 10 with correct prefix")
    void validateRoomNumberFloorPrefix_whenFloor10WithCorrectPrefix_doesNotThrow() {
        // Given
        String roomNumber = "1001";
        Integer floor = 10;

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));
    }

    @Test
    @DisplayName("Should throw exception when room number has wrong floor prefix")
    void validateRoomNumberFloorPrefix_whenWrongFloorPrefix_throwsException() {
        // Given
        String roomNumber = "201"; // Room number for floor 2
        Integer floor = 3; // But we're on floor 3

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("must start with floor number '3'"),
                () -> assertThat(exception.getMessage()).contains("201"));
    }

    @Test
    @DisplayName("Should throw exception when room number is just the floor number")
    void validateRoomNumberFloorPrefix_whenOnlyFloorNumber_throwsException() {
        // Given
        String roomNumber = "5"; // Just the floor number, no room suffix
        Integer floor = 5;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("must have at least 3 digits"));
    }

    @Test
    @DisplayName("Should throw exception when room number has only 1 digit after floor prefix")
    void validateRoomNumberFloorPrefix_whenOnlyOneDigitAfterPrefix_throwsException() {
        // Given
        String roomNumber = "51"; // Floor 5, but only 1 digit after (should be 501)
        Integer floor = 5;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("must have at least 3 digits"));
    }

    @Test
    @DisplayName("Should not throw exception when room number has more than 2 digits after floor prefix")
    void validateRoomNumberFloorPrefix_whenMoreThanTwoDigitsAfterPrefix_doesNotThrow() {
        // Given
        String roomNumber = "5123"; // Floor 5 with 3 digits after prefix (now valid)
        Integer floor = 5;

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));
    }

    @Test
    @DisplayName("Should throw exception when room number is null")
    void validateRoomNumberFloorPrefix_whenRoomNumberIsNull_throwsException() {
        // Given
        String roomNumber = null;
        Integer floor = 1;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("cannot be null or empty"));
    }

    @Test
    @DisplayName("Should throw exception when room number is empty")
    void validateRoomNumberFloorPrefix_whenRoomNumberIsEmpty_throwsException() {
        // Given
        String roomNumber = "";
        Integer floor = 1;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("cannot be null or empty"));
    }

    @Test
    @DisplayName("Should throw exception when floor is null")
    void validateRoomNumberFloorPrefix_whenFloorIsNull_throwsException() {
        // Given
        String roomNumber = "101";
        Integer floor = null;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("floor"),
                () -> assertThat(exception.getMessage()).contains("Floor number cannot be null"));
    }

    @Test
    @DisplayName("Should throw exception when room number doesn't start with floor number")
    void validateRoomNumberFloorPrefix_whenNoFloorPrefix_throwsException() {
        // Given
        String roomNumber = "ABC"; // Alphabetic room number
        Integer floor = 1;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("must start with floor number '1'"));
    }

    @Test
    @DisplayName("Should not throw exception for multi-digit floor with correct prefix and 2 room digits")
    void validateRoomNumberFloorPrefix_whenMultiDigitFloor_doesNotThrow() {
        // Given - Floor 150 + 2 digits = 15001 (5 total digits)
        String roomNumber = "15001";
        Integer floor = 150;

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));
    }

    @Test
    @DisplayName("Should throw exception when floor 10 room number has only 1 digit after prefix")
    void validateRoomNumberFloorPrefix_whenFloor10WithOnlyOneDigitAfterPrefix_throwsException() {
        // Given - "101" starts with "10" but only has 1 digit after the prefix
        // This would be ambiguous: is "101" floor 1 room 01, or floor 10 room 1?
        // We require floor 10 room numbers to have at least 2 digits after prefix (e.g., 1001, 1002)
        String roomNumber = "101";
        Integer floor = 10;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("must have at least 4 digits"));
    }

    @Test
    @DisplayName("Should throw exception when room number has letters after floor prefix")
    void validateRoomNumberFloorPrefix_whenLettersAfterPrefix_throwsException() {
        // Given
        String roomNumber = "1AB"; // Floor 1 + letters
        Integer floor = 1;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("roomNumber"),
                () -> assertThat(exception.getMessage()).contains("must have only digits after floor prefix"));
    }

    @Test
    @DisplayName("Should not throw exception for room numbers with leading zeros in room suffix")
    void validateRoomNumberFloorPrefix_whenRoomNumberWithLeadingZero_doesNotThrow() {
        // Given - Room 01, 02, 03, etc. are valid
        String roomNumber = "101"; // Floor 1, Room 01
        Integer floor = 1;

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));
    }

    @Test
    @DisplayName("Should not throw exception for floor 0 with correct format")
    void validateRoomNumberFloorPrefix_whenFloor0WithCorrectFormat_doesNotThrow() {
        // Given - Floor 0, Room 01 = "001"
        String roomNumber = "001";
        Integer floor = 0;

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomNumberFloorPrefix(roomNumber, floor));
    }

    // ==================== validateDateRange Tests ====================

    @Test
    @DisplayName("validateDateRange should not throw exception when checkout date is after checkin date")
    public void validateDateRange_whenCheckoutAfterCheckin_doesNotThrow() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateDateRange(checkInDate, checkOutDate));
    }

    @Test
    @DisplayName("validateDateRange should throw BusinessValidationException when checkout date is before checkin date")
    public void validateDateRange_whenCheckoutBeforeCheckin_throwsBusinessValidationException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateDateRange(checkInDate, checkOutDate));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Check-out date must be after check-in date"),
                () -> assertThat(exception.getField()).isEqualTo("checkOutDate"));
    }

    @Test
    @DisplayName("validateDateRange should throw BusinessValidationException when checkout date equals checkin date")
    public void validateDateRange_whenCheckoutEqualsCheckin_throwsBusinessValidationException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(5);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateDateRange(checkInDate, checkOutDate));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Check-out date must be after check-in date"),
                () -> assertThat(exception.getField()).isEqualTo("checkOutDate"));
    }

    @Test
    @DisplayName("validateDateRange should throw BusinessValidationException when checkin date is null")
    public void validateDateRange_whenCheckinIsNull_throwsBusinessValidationException() {
        // Given
        LocalDate checkInDate = null;
        LocalDate checkOutDate = LocalDate.now().plusDays(3);

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateDateRange(checkInDate, checkOutDate));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Check-in date cannot be null"),
                () -> assertThat(exception.getField()).isEqualTo("checkInDate"));
    }

    @Test
    @DisplayName("validateDateRange should throw BusinessValidationException when checkout date is null")
    public void validateDateRange_whenCheckoutIsNull_throwsBusinessValidationException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = null;

        // When & Then
        BusinessValidationException exception = assertThrows(BusinessValidationException.class,
                () -> roomValidator.validateDateRange(checkInDate, checkOutDate));

        assertAll(() -> assertThat(exception.getMessage()).isEqualTo("Check-out date cannot be null"),
                () -> assertThat(exception.getField()).isEqualTo("checkOutDate"));
    }

    @Test
    @DisplayName("validateDateRange should not throw exception for past dates when checkout is after checkin")
    public void validateDateRange_whenPastDatesWithValidRange_doesNotThrow() {
        // Given
        LocalDate checkInDate = LocalDate.now().minusDays(5);
        LocalDate checkOutDate = LocalDate.now().minusDays(3);

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateDateRange(checkInDate, checkOutDate));
    }

    @Test
    @DisplayName("validateDateRange should not throw exception when dates are far in the future")
    public void validateDateRange_whenFutureDatesWithValidRange_doesNotThrow() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(100);
        LocalDate checkOutDate = LocalDate.now().plusDays(105);

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateDateRange(checkInDate, checkOutDate));
    }

    // ==================== validateRoomUpdate Tests ====================

    @Test
    @DisplayName("Should not throw exception when capacity matches bed count using overrides only")
    void validateRoomUpdate_whenCapacityMatchesBedCountWithOverrides_doesNotThrow() {
        // Given
        RoomType roomType = RoomType.builder().capacity(2).singleBed(2).doubleBed(0).build();

        tuwien.taskinn.dto.request.RoomRequestDto dto = tuwien.taskinn.dto.request.RoomRequestDto.builder()
                .capacityOverride(3) // 1 single + 1 double = 1 + 2*1 = 3
                .singleBedOverride(1)
                .doubleBedOverride(1)
                .build();

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomUpdate(dto, roomType));
    }

    @Test
    @DisplayName("Should not throw exception when capacity matches bed count using room type defaults")
    void validateRoomUpdate_whenCapacityMatchesBedCountWithDefaults_doesNotThrow() {
        // Given
        RoomType roomType = RoomType.builder()
                .capacity(4) // 2 single + 1 double = 2 + 2*1 = 4
                .singleBed(2)
                .doubleBed(1)
                .build();

        tuwien.taskinn.dto.request.RoomRequestDto dto = tuwien.taskinn.dto.request.RoomRequestDto.builder()
                .capacityOverride(null)
                .singleBedOverride(null)
                .doubleBedOverride(null)
                .build();

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomUpdate(dto, roomType));
    }

    @Test
    @DisplayName("Should not throw exception when capacity matches with mixed overrides and defaults")
    void validateRoomUpdate_whenCapacityMatchesWithMixedValues_doesNotThrow() {
        // Given
        RoomType roomType = RoomType.builder().capacity(4).singleBed(2).doubleBed(1).build();

        tuwien.taskinn.dto.request.RoomRequestDto dto = tuwien.taskinn.dto.request.RoomRequestDto.builder()
                .capacityOverride(5) // 1 single (override) + 2 double (override) = 1 + 2*2 = 5
                .singleBedOverride(1)
                .doubleBedOverride(2)
                .build();

        // When & Then
        assertDoesNotThrow(() -> roomValidator.validateRoomUpdate(dto, roomType));
    }

    @Test
    @DisplayName("Should throw exception when capacity is too low for bed count")
    void validateRoomUpdate_whenCapacityTooLow_throwsException() {
        // Given
        RoomType roomType = RoomType.builder().capacity(4).singleBed(2).doubleBed(1).build();

        tuwien.taskinn.dto.request.RoomRequestDto dto = tuwien.taskinn.dto.request.RoomRequestDto.builder()
                .capacityOverride(2) // Should be 4 (2 single + 1 double = 2 + 2*1 = 4)
                .singleBedOverride(null) // Uses default: 2
                .doubleBedOverride(null) // Uses default: 1
                .build();

        // When & Then
        BusinessValidationException exception =
                assertThrows(BusinessValidationException.class, () -> roomValidator.validateRoomUpdate(dto, roomType));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("capacityOverride"),
                () -> assertThat(exception.getMessage())
                        .contains("Capacity must equal the sum of single and double beds"));
    }

    @Test
    @DisplayName("Should throw exception when capacity is too high for bed count")
    void validateRoomUpdate_whenCapacityTooHigh_throwsException() {
        // Given
        RoomType roomType = RoomType.builder().capacity(4).singleBed(2).doubleBed(1).build();

        tuwien.taskinn.dto.request.RoomRequestDto dto = tuwien.taskinn.dto.request.RoomRequestDto.builder()
                .capacityOverride(null) // Uses default: 4
                .singleBedOverride(1)
                .doubleBedOverride(1) // Should be 3 (1 + 2*1 = 3), but capacity is 4
                .build();

        // When & Then
        BusinessValidationException exception =
                assertThrows(BusinessValidationException.class, () -> roomValidator.validateRoomUpdate(dto, roomType));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("capacityOverride"),
                () -> assertThat(exception.getMessage())
                        .contains("Capacity must equal the sum of single and double beds"));
    }

    @Test
    @DisplayName("Should throw exception when beds are overridden but capacity doesn't match")
    void validateRoomUpdate_whenBedsOverriddenButCapacityDoesNotMatch_throwsException() {
        // Given
        RoomType roomType = RoomType.builder().capacity(4).singleBed(2).doubleBed(1).build();

        tuwien.taskinn.dto.request.RoomRequestDto dto = tuwien.taskinn.dto.request.RoomRequestDto.builder()
                .capacityOverride(null) // Uses default: 4
                .singleBedOverride(2)
                .doubleBedOverride(2) // 2 + 2*2 = 6, but capacity is 4
                .build();

        // When & Then
        BusinessValidationException exception =
                assertThrows(BusinessValidationException.class, () -> roomValidator.validateRoomUpdate(dto, roomType));

        assertAll(() -> assertThat(exception.getField()).isEqualTo("capacityOverride"),
                () -> assertThat(exception.getMessage())
                        .contains("Capacity must equal the sum of single and double beds"));
    }
}
