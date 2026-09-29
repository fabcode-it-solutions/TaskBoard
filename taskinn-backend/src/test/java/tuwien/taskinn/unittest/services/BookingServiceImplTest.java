package tuwien.taskinn.unittest.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.context.ApplicationEventPublisher;
import tuwien.taskinn.config.FileStorageConfig;
import tuwien.taskinn.dto.request.*;
import tuwien.taskinn.dto.response.*;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.entity.RoomType;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.mapper.BookingMapper;
import tuwien.taskinn.mapper.RoomMapper;
import tuwien.taskinn.mapper.RoomTypeMapper;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.dto.response.CreateOrUpdateBookingResponseDto;
import tuwien.taskinn.services.EmailService;
import tuwien.taskinn.services.impl.BookingServiceImpl;
import tuwien.taskinn.services.validator.BookingValidator;
import tuwien.taskinn.services.validator.CheckInValidator;
import tuwien.taskinn.util.EncryptionUtil;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Guest;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.GuestRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Unit Tests")
public class BookingServiceImplTest {

    @Mock
    private BookingValidator bookingValidator;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private GuestRepository guestRepository;
    @Mock
    private CheckInValidator checkInValidator;
    @Mock
    private EncryptionUtil encryptionUtil;
    @Mock
    private FileStorageConfig fileStorageConfig;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private RoomTypeMapper roomTypeMapper;

    @Mock
    private tuwien.taskinn.repo.RoomTypeRepository roomTypeRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("getRoomTypeOptions should validate request and return aggregated room type options")
    public void getRoomTypeOptions_whenValidRequest_returnsRoomTypeOptions() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 2;
        Integer numAdults = 4;
        Integer numChildren = 2;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType roomType =
                RoomType.builder().id(1L).name("Family Suite").capacity(6).basePrice(new BigDecimal("200.00")).build();

        Room room1 = Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(roomType).build();
        Room room2 = Room.builder().id(102L).roomNumber("102").status(RoomStatus.AVAILABLE).roomType(roomType).build();

        RoomTypeDto roomTypeDto = RoomTypeDto.builder()
                .id(1L)
                .name("Family Suite")
                .capacity(6)
                .basePrice(new BigDecimal("200.00"))
                .build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(room1, room2));

        when(roomTypeMapper.toDto(roomType)).thenReturn(roomTypeDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Then
        verify(bookingValidator).validateRoomOptionsRequestDto(requestDto);
        verify(roomRepository).findAvailableRoomsByDateRange(checkInDate, checkOutDate);
        // When 2 rooms are explicitly requested, algorithm selects 2 rooms of the same type
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(1),
                () -> assertThat(result.getFirst().getRoomType().getName()).isEqualTo("Family Suite"),
                () -> assertThat(result.getFirst().getAvailableCount()).isEqualTo(2),
                () -> assertThat(result.getFirst().getRecommendedCount()).isEqualTo(2),
                () -> assertThat(result.getFirst().getTotalPricePerNight()).isEqualTo(new BigDecimal("400.00")));
    }

    @Test
    @DisplayName("getRoomTypeOptions should split options by room type and override price")
    public void getRoomTypeOptions_whenOverridesPresent_groupsByTypeAndPrice() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 2;
        Integer numAdults = 2;
        Integer numChildren = 0;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType singleRoom =
                RoomType.builder().id(1L).name("Single Room").capacity(1).basePrice(new BigDecimal("80.00")).build();

        List<Room> availableRooms = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            availableRooms.add(Room.builder()
                    .id((long) (100 + i))
                    .roomNumber(String.valueOf(100 + i))
                    .status(RoomStatus.AVAILABLE)
                    .roomType(singleRoom)
                    .build());
        }
        for (int i = 1; i <= 3; i++) {
            availableRooms.add(Room.builder()
                    .id((long) (200 + i))
                    .roomNumber(String.valueOf(200 + i))
                    .status(RoomStatus.AVAILABLE)
                    .basePriceOverride(new BigDecimal("90.00"))
                    .roomType(singleRoom)
                    .build());
        }

        RoomTypeDto singleDto =
                RoomTypeDto.builder().id(1L).name("Single Room").capacity(1).basePrice(new BigDecimal("80.00")).build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(availableRooms);
        when(roomTypeMapper.toDto(singleRoom)).thenReturn(singleDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Then
        assertAll(() -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(2),
                () -> assertThat(result.get(0).getPricePerNight()).isEqualTo(new BigDecimal("80.00")),
                () -> assertThat(result.get(0).getAvailableCount()).isEqualTo(6),
                () -> assertThat(result.get(0).getRecommendedCount()).isEqualTo(2),
                () -> assertThat(result.get(0).getTotalPricePerNight()).isEqualTo(new BigDecimal("160.00")),
                () -> assertThat(result.get(1).getPricePerNight()).isEqualTo(new BigDecimal("90.00")),
                () -> assertThat(result.get(1).getAvailableCount()).isEqualTo(3),
                () -> assertThat(result.get(1).getRecommendedCount()).isEqualTo(0));
    }

    @Test
    @DisplayName("getRoomTypeOptions should return empty list when no rooms are available")
    public void getRoomTypeOptions_whenNoRoomsAvailable_returnsEmptyList() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 1;
        Integer numAdults = 2;
        Integer numChildren = 0;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Then
        verify(bookingValidator).validateRoomOptionsRequestDto(requestDto);
        verify(roomRepository).findAvailableRoomsByDateRange(checkInDate, checkOutDate);
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result).isEmpty());
    }

    @Test
    @DisplayName("checkInBooking should correctly create encrypted files")
    void givenValidBookingAndGuest_whenCheckInBooking_thenFileCreatedAndCleanedUp() throws Exception {

        Long bookingId = 10000L;
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .checkInDate(LocalDate.now())
                .numAdults(1)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(encryptionUtil.encrypt(any())).thenReturn("encrypted-content".getBytes());
        when(fileStorageConfig.getStorageDir())
                .thenReturn(System.getProperty("java.io.tmpdir") + "/taskinn-test-files");

        CheckInGuestDto guestDto = new CheckInGuestDto();
        guestDto.setFirstName("A");
        guestDto.setLastName("B");
        guestDto.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestDto.setRoomId(1L);
        guestDto.setEmail("mail@example.com");
        guestDto.setPhoneNumber("123");

        List<CheckInGuestDto> guests = List.of(guestDto);

        MockMultipartFile mockFile =
                new MockMultipartFile("guestFiles_0", "test-image.jpg", "image/jpeg", "test-image-content".getBytes());
        Map<String, MultipartFile> files = Map.of("guestFiles_0", mockFile);

        bookingService.checkInBooking(bookingId, guests, files);
        Path idsRoot = Path.of(fileStorageConfig.getStorageDir());

        Path idsFolder = idsRoot.resolve("ids");
        String expectedName = "booking10000_guest1_id.enc";
        Path expectedFile = idsFolder.resolve(expectedName);
        assertAll(() -> assertTrue(Files.exists(expectedFile), "Encrypted image file should exist"),
                () -> assertArrayEquals(Files.readAllBytes(expectedFile), "encrypted-content".getBytes()));

        // Cleanup
        if (Files.exists(idsFolder) && Files.isDirectory(idsFolder)) {
            try (var stream = Files.list(idsFolder)) {
                stream.forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            Files.deleteIfExists(idsFolder);
        }
        Files.deleteIfExists(idsRoot);
    }

    @Test
    @DisplayName("checkInBooking should update id, email and phone of existing guests")
    void givenExistingGuest_whenCheckInBooking_thenGuestUpdatedAndFileCleanedUp() throws Exception {
        // Given
        Long bookingId = 200000L;
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .checkInDate(LocalDate.now())
                .numAdults(1)
                .build();
        Guest existingGuest = Guest.builder()
                .id(19L)
                .firstName("C")
                .lastName("D")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .email("old@example.com")
                .phone("321")
                .idImageUrl("oldfile.enc")
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("C", "D",
                LocalDate.of(1985, 5, 5))).thenReturn(Optional.of(existingGuest));
        when(encryptionUtil.encrypt(any())).thenReturn("encrypted-content".getBytes());
        when(fileStorageConfig.getStorageDir())
                .thenReturn(System.getProperty("java.io.tmpdir") + "/taskinn-test-files");

        CheckInGuestDto guestDto = new CheckInGuestDto();
        guestDto.setFirstName("C");
        guestDto.setLastName("D");
        guestDto.setDateOfBirth(LocalDate.of(1985, 5, 5));
        guestDto.setRoomId(1L);
        guestDto.setEmail("new@example.com");
        guestDto.setPhoneNumber("999");

        List<CheckInGuestDto> guests = List.of(guestDto);

        MockMultipartFile mockFile =
                new MockMultipartFile("guestFiles_0", "test-image.jpg", "image/jpeg", "test-image-content".getBytes());
        Map<String, MultipartFile> files = Map.of("guestFiles_0", mockFile);

        bookingService.checkInBooking(bookingId, guests, files);
        Path idsRoot = Path.of(fileStorageConfig.getStorageDir());

        Path idsFolder = idsRoot.resolve("ids");
        Path expectedFile = idsFolder.resolve("booking200000_guest1_id.enc");
        assertAll(() -> assertTrue(Files.exists(expectedFile)),
                () -> assertEquals("new@example.com", existingGuest.getEmail()),
                () -> assertEquals("999", existingGuest.getPhone()));

        // Cleanup
        if (Files.exists(idsFolder) && Files.isDirectory(idsFolder)) {
            try (var stream = Files.list(idsFolder)) {
                stream.forEach(file -> {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
            Files.deleteIfExists(idsFolder);
        }
        Files.deleteIfExists(idsRoot);
    }

    @Test
    @DisplayName("getRoomsForCheckinByBookingId should return booked rooms when called correctly")
    void givenValidBookingId_whenCalled_thenReturnsRooms() {
        // Given
        Long bookingId = 1L;
        RoomForCheckinResponseDto room = new RoomForCheckinResponseDto();
        List<RoomForCheckinResponseDto> expectedRooms = List.of(room);

        when(bookingRepository.findRoomsByBookingId(bookingId)).thenReturn(expectedRooms);

        // When
        List<RoomForCheckinResponseDto> result = bookingService.getRoomsForCheckinByBookingId(bookingId);

        // Then
        assertAll(() -> verify(checkInValidator).validateBookingStatusForCheckin(bookingId),
                () -> verify(bookingRepository, times(1)).findRoomsByBookingId(bookingId), () -> assertNotNull(result),
                () -> assertEquals(1, result.size()));
    }

    @Test
    @DisplayName("getRoomsForCheckinByBookingId should throw an exception if booking doesnt exist")
    void givenInvalidBookingId_thenReturnsEmptyList() {
        // Given
        Long invalidBookingId = 99L;
        // Simulate validator throwing when booking is not found
        doThrow(new EntityNotFoundException("Booking not found")).when(checkInValidator)
                .validateBookingStatusForCheckin(invalidBookingId);

        // When & Then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> bookingService.getRoomsForCheckinByBookingId(invalidBookingId));
        assertAll(() -> assertEquals("Booking not found", exception.getMessage()),
                () -> verify(checkInValidator).validateBookingStatusForCheckin(invalidBookingId),
                () -> verify(bookingRepository, never()).findRoomsByBookingId(any()));
    }

    @Test
    @DisplayName("getRoomTypeOptions should aggregate multiple room types and recommend best option")
    public void getRoomTypeOptions_whenMultipleRoomTypesAvailable_returnsAggregatedOptions() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 1;
        Integer numAdults = 2;
        Integer numChildren = 1;
        // Total guests: 3

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType standardRoom =
                RoomType.builder().id(1L).name("Standard Room").capacity(2).basePrice(new BigDecimal("100.00")).build();

        RoomType deluxeRoom =
                RoomType.builder().id(2L).name("Deluxe Room").capacity(4).basePrice(new BigDecimal("150.00")).build();

        RoomType suiteRoom =
                RoomType.builder().id(3L).name("Suite").capacity(6).basePrice(new BigDecimal("250.00")).build();

        // 2 standard rooms, 3 deluxe rooms, 1 suite
        Room room1 =
                Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(standardRoom).build();
        Room room2 =
                Room.builder().id(102L).roomNumber("102").status(RoomStatus.AVAILABLE).roomType(standardRoom).build();
        Room room3 =
                Room.builder().id(103L).roomNumber("103").status(RoomStatus.AVAILABLE).roomType(deluxeRoom).build();
        Room room4 =
                Room.builder().id(104L).roomNumber("104").status(RoomStatus.AVAILABLE).roomType(deluxeRoom).build();
        Room room5 =
                Room.builder().id(105L).roomNumber("105").status(RoomStatus.AVAILABLE).roomType(deluxeRoom).build();
        Room room6 = Room.builder().id(106L).roomNumber("106").status(RoomStatus.AVAILABLE).roomType(suiteRoom).build();

        RoomTypeDto standardDto = RoomTypeDto.builder()
                .id(1L)
                .name("Standard Room")
                .capacity(2)
                .basePrice(new BigDecimal("100.00"))
                .build();
        RoomTypeDto deluxeDto = RoomTypeDto.builder()
                .id(2L)
                .name("Deluxe Room")
                .capacity(4)
                .basePrice(new BigDecimal("150.00"))
                .build();
        RoomTypeDto suiteDto =
                RoomTypeDto.builder().id(3L).name("Suite").capacity(6).basePrice(new BigDecimal("250.00")).build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(room1, room2, room3, room4, room5, room6));

        // Mock all room types that will be returned
        when(roomTypeMapper.toDto(standardRoom)).thenReturn(standardDto);
        when(roomTypeMapper.toDto(deluxeRoom)).thenReturn(deluxeDto);
        when(roomTypeMapper.toDto(suiteRoom)).thenReturn(suiteDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Then
        verify(bookingValidator).validateRoomOptionsRequestDto(requestDto);
        verify(roomRepository).findAvailableRoomsByDateRange(checkInDate, checkOutDate);

        // Should return all available room types sorted by price (cheapest first)
        // Algorithm selects Deluxe Room as best fit (capacity 4 for 3 guests)
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(3),
                // First should be Standard Room (cheapest at 100.00)
                () -> assertThat(result.get(0).getRoomType().getName()).isEqualTo("Standard Room"),
                () -> assertThat(result.get(0).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("100.00")),
                () -> assertThat(result.get(0).getAvailableCount()).isEqualTo(2),
                () -> assertThat(result.get(0).getRecommendedCount()).isEqualTo(0),
                // Second should be Deluxe Room (recommended, price 150.00)
                () -> assertThat(result.get(1).getRoomType().getName()).isEqualTo("Deluxe Room"),
                () -> assertThat(result.get(1).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("150.00")),
                () -> assertThat(result.get(1).getAvailableCount()).isEqualTo(3),
                () -> assertThat(result.get(1).getRecommendedCount()).isEqualTo(1),
                () -> assertThat(result.get(1).getTotalPricePerNight()).isEqualTo(new BigDecimal("150.00")),
                // Third should be Suite (most expensive at 250.00)
                () -> assertThat(result.get(2).getRoomType().getName()).isEqualTo("Suite"),
                () -> assertThat(result.get(2).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("250.00")),
                () -> assertThat(result.get(2).getAvailableCount()).isEqualTo(1),
                () -> assertThat(result.get(2).getRecommendedCount()).isEqualTo(0),
                () -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> verify(roomRepository).findAvailableRoomsByDateRange(checkInDate, checkOutDate));
    }

    @Test
    @DisplayName("getRoomTypeOptions should handle single adult booking with best fit selection")
    public void getRoomTypeOptions_whenSingleAdult_selectsSmallestSuitableRoom() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 1;
        Integer numAdults = 1;
        Integer numChildren = 0;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType singleRoom = RoomType.builder()
                .id(1L)
                .name("Single Room")
                .capacity(1)
                .basePrice(new BigDecimal("80.00"))
                .singleBed(1)
                .build();

        RoomType doubleRoom = RoomType.builder()
                .id(2L)
                .name("Double Room")
                .capacity(2)
                .basePrice(new BigDecimal("120.00"))
                .doubleBed(1)
                .build();

        Room room1 =
                Room.builder().id(201L).roomNumber("201").status(RoomStatus.AVAILABLE).roomType(singleRoom).build();
        Room room2 =
                Room.builder().id(202L).roomNumber("202").status(RoomStatus.AVAILABLE).roomType(doubleRoom).build();

        RoomTypeDto singleDto = RoomTypeDto.builder()
                .id(1L)
                .name("Single Room")
                .capacity(1)
                .basePrice(new BigDecimal("80.00"))
                .singleBed(1)
                .build();
        RoomTypeDto doubleDto = RoomTypeDto.builder()
                .id(2L)
                .name("Double Room")
                .capacity(2)
                .basePrice(new BigDecimal("120.00"))
                .doubleBed(1)
                .build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(room1, room2));

        // Mock all room types that will be returned
        when(roomTypeMapper.toDto(singleRoom)).thenReturn(singleDto);
        when(roomTypeMapper.toDto(doubleRoom)).thenReturn(doubleDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Then
        verify(bookingValidator).validateRoomOptionsRequestDto(requestDto);
        verify(roomRepository).findAvailableRoomsByDateRange(checkInDate, checkOutDate);

        // Should return all available room types sorted by price (cheapest first)
        // Single Room is exact capacity match and cheapest
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(2),
                // First should be Single Room (cheapest, recommended)
                () -> assertThat(result.get(0).getRoomType().getName()).isEqualTo("Single Room"),
                () -> assertThat(result.get(0).getRoomType().getCapacity()).isEqualTo(1),
                () -> assertThat(result.get(0).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("80.00")),
                () -> assertThat(result.get(0).getAvailableCount()).isEqualTo(1),
                () -> assertThat(result.get(0).getRecommendedCount()).isEqualTo(1),
                () -> assertThat(result.get(0).getTotalPricePerNight()).isEqualTo(new BigDecimal("80.00")),
                // Second should be Double Room (more expensive)
                () -> assertThat(result.get(1).getRoomType().getName()).isEqualTo("Double Room"),
                () -> assertThat(result.get(1).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("120.00")),
                () -> assertThat(result.get(1).getAvailableCount()).isEqualTo(1),
                () -> assertThat(result.get(1).getRecommendedCount()).isEqualTo(0),
                () -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> verify(roomRepository).findAvailableRoomsByDateRange(checkInDate, checkOutDate));
    }

    @Test
    @DisplayName("getRoomTypeOptions should recommend exact room type match when capacity is perfect")
    public void getRoomTypeOptions_whenExactCapacityMatch_selectsExactType() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 1;
        Integer numAdults = 4;
        Integer numChildren = 0;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType quadRoom = RoomType.builder()
                .id(1L)
                .name("Quad Room")
                .capacity(4)
                .basePrice(new BigDecimal("180.00"))
                .doubleBed(2)
                .build();

        RoomType familyRoom = RoomType.builder()
                .id(2L)
                .name("Family Room")
                .capacity(6)
                .basePrice(new BigDecimal("250.00"))
                .doubleBed(2)
                .singleBed(2)
                .build();

        Room room1 = Room.builder().id(301L).roomNumber("301").status(RoomStatus.AVAILABLE).roomType(quadRoom).build();
        Room room2 =
                Room.builder().id(401L).roomNumber("401").status(RoomStatus.AVAILABLE).roomType(familyRoom).build();

        RoomTypeDto quadDto = RoomTypeDto.builder()
                .id(1L)
                .name("Quad Room")
                .capacity(4)
                .basePrice(new BigDecimal("180.00"))
                .doubleBed(2)
                .build();
        RoomTypeDto familyDto = RoomTypeDto.builder()
                .id(2L)
                .name("Family Room")
                .capacity(6)
                .basePrice(new BigDecimal("250.00"))
                .doubleBed(2)
                .singleBed(2)
                .build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(room1, room2));

        // Mock all room types that will be returned
        when(roomTypeMapper.toDto(quadRoom)).thenReturn(quadDto);
        when(roomTypeMapper.toDto(familyRoom)).thenReturn(familyDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Should return all available room types sorted by price (cheapest first)
        // Quad Room is exact capacity match and cheaper
        assertAll(() -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(2),
                // First should be Quad Room (cheaper, recommended)
                () -> assertThat(result.get(0).getRoomType().getName()).isEqualTo("Quad Room"),
                () -> assertThat(result.get(0).getRoomType().getCapacity()).isEqualTo(4),
                () -> assertThat(result.get(0).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("180.00")),
                () -> assertThat(result.get(0).getAvailableCount()).isEqualTo(1),
                () -> assertThat(result.get(0).getRecommendedCount()).isEqualTo(1),
                () -> assertThat(result.get(0).getTotalPricePerNight()).isEqualTo(new BigDecimal("180.00")),
                // Second should be Family Room (more expensive)
                () -> assertThat(result.get(1).getRoomType().getName()).isEqualTo("Family Room"),
                () -> assertThat(result.get(1).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("250.00")),
                () -> assertThat(result.get(1).getAvailableCount()).isEqualTo(1),
                () -> assertThat(result.get(1).getRecommendedCount()).isEqualTo(0));
    }

    @Test
    @DisplayName("getRoomTypeOptions should recommend multiple rooms for large groups")
    public void getRoomTypeOptions_whenLargeGroup_recommendsMultipleRooms() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 3;
        Integer numAdults = 8;
        Integer numChildren = 2;
        // Total guests: 10

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType doubleRoom =
                RoomType.builder().id(1L).name("Double Room").capacity(2).basePrice(new BigDecimal("120.00")).build();

        RoomType quadRoom =
                RoomType.builder().id(2L).name("Quad Room").capacity(4).basePrice(new BigDecimal("180.00")).build();

        // 2 double rooms, 4 quad rooms available
        Room room1 =
                Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(doubleRoom).build();
        Room room2 =
                Room.builder().id(102L).roomNumber("102").status(RoomStatus.AVAILABLE).roomType(doubleRoom).build();
        Room room3 = Room.builder().id(201L).roomNumber("201").status(RoomStatus.AVAILABLE).roomType(quadRoom).build();
        Room room4 = Room.builder().id(202L).roomNumber("202").status(RoomStatus.AVAILABLE).roomType(quadRoom).build();
        Room room5 = Room.builder().id(203L).roomNumber("203").status(RoomStatus.AVAILABLE).roomType(quadRoom).build();
        Room room6 = Room.builder().id(204L).roomNumber("204").status(RoomStatus.AVAILABLE).roomType(quadRoom).build();

        RoomTypeDto doubleDto = RoomTypeDto.builder()
                .id(1L)
                .name("Double Room")
                .capacity(2)
                .basePrice(new BigDecimal("120.00"))
                .build();
        RoomTypeDto quadDto =
                RoomTypeDto.builder().id(2L).name("Quad Room").capacity(4).basePrice(new BigDecimal("180.00")).build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(room1, room2, room3, room4, room5, room6));

        // Mock all room types that will be returned
        when(roomTypeMapper.toDto(doubleRoom)).thenReturn(doubleDto);
        when(roomTypeMapper.toDto(quadRoom)).thenReturn(quadDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Should return all available room types sorted by price (cheapest first)
        // Algorithm selects 3 Quad Rooms (exactly what was requested, capacity 12 for 10 guests)
        assertAll(() -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(2),
                // First should be Double Room (cheaper at 120.00)
                () -> assertThat(result.get(0).getRoomType().getName()).isEqualTo("Double Room"),
                () -> assertThat(result.get(0).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("120.00")),
                () -> assertThat(result.get(0).getAvailableCount()).isEqualTo(2),
                () -> assertThat(result.get(0).getRecommendedCount()).isEqualTo(0),
                // Second should be Quad Room (recommended, price 180.00)
                () -> assertThat(result.get(1).getRoomType().getName()).isEqualTo("Quad Room"),
                () -> assertThat(result.get(1).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("180.00")),
                () -> assertThat(result.get(1).getAvailableCount()).isEqualTo(4),
                () -> assertThat(result.get(1).getRecommendedCount()).isEqualTo(3),
                () -> assertThat(result.get(1).getTotalPricePerNight()).isEqualTo(new BigDecimal("540.00")));
    }

    @Test
    @DisplayName("getRoomTypeOptions should select best combination when no single room fits")
    public void getRoomTypeOptions_whenNeedsCombination_selectsBestCombination() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 1; // User wants 1 room but...
        Integer numAdults = 5;
        Integer numChildren = 0;
        // Total guests: 5 - no single room fits, needs combination

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType doubleRoom =
                RoomType.builder().id(1L).name("Double Room").capacity(2).basePrice(new BigDecimal("120.00")).build();

        RoomType tripleRoom =
                RoomType.builder().id(2L).name("Triple Room").capacity(3).basePrice(new BigDecimal("150.00")).build();

        // No room with capacity 5+, so algorithm will find combination
        Room room1 =
                Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(doubleRoom).build();
        Room room2 =
                Room.builder().id(102L).roomNumber("102").status(RoomStatus.AVAILABLE).roomType(doubleRoom).build();
        Room room3 =
                Room.builder().id(201L).roomNumber("201").status(RoomStatus.AVAILABLE).roomType(tripleRoom).build();
        Room room4 =
                Room.builder().id(202L).roomNumber("202").status(RoomStatus.AVAILABLE).roomType(tripleRoom).build();

        RoomTypeDto doubleDto = RoomTypeDto.builder()
                .id(1L)
                .name("Double Room")
                .capacity(2)
                .basePrice(new BigDecimal("120.00"))
                .build();
        RoomTypeDto tripleDto = RoomTypeDto.builder()
                .id(2L)
                .name("Triple Room")
                .capacity(3)
                .basePrice(new BigDecimal("150.00"))
                .build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(room1, room2, room3, room4));

        // Mock all room types that will be returned
        when(roomTypeMapper.toDto(doubleRoom)).thenReturn(doubleDto);
        when(roomTypeMapper.toDto(tripleRoom)).thenReturn(tripleDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Should return all available room types sorted by price (cheapest first)
        // Algorithm selects 2 Triple Rooms (2x3 = 6 capacity for 5 guests)
        assertAll(() -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(2),
                // First should be Double Room (cheaper at 120.00)
                () -> assertThat(result.get(0).getRoomType().getName()).isEqualTo("Double Room"),
                () -> assertThat(result.get(0).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("120.00")),
                () -> assertThat(result.get(0).getAvailableCount()).isEqualTo(2),
                () -> assertThat(result.get(0).getRecommendedCount()).isEqualTo(0),
                // Second should be Triple Room (recommended, price 150.00)
                () -> assertThat(result.get(1).getRoomType().getName()).isEqualTo("Triple Room"),
                () -> assertThat(result.get(1).getRoomType().getBasePrice()).isEqualTo(new BigDecimal("150.00")),
                () -> assertThat(result.get(1).getAvailableCount()).isEqualTo(2),
                () -> assertThat(result.get(1).getRecommendedCount()).isEqualTo(2),
                () -> assertThat(result.get(1).getTotalPricePerNight()).isEqualTo(new BigDecimal("300.00")));
    }

    @Test
    @DisplayName("getRoomTypeOptions should handle scenario when only one room type available")
    public void getRoomTypeOptions_whenSingleRoomType_aggregatesCorrectly() {

        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        Integer numRooms = 2;
        Integer numAdults = 3;
        Integer numChildren = 1;

        RoomOptionsRequestDto requestDto =
                new RoomOptionsRequestDto(checkInDate, checkOutDate, numRooms, numAdults, numChildren);

        RoomType standardRoom =
                RoomType.builder().id(1L).name("Standard Room").capacity(2).basePrice(new BigDecimal("100.00")).build();

        // 10 rooms of the same type
        List<Room> rooms = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            rooms.add(Room.builder()
                    .id((long) (100 + i))
                    .roomNumber(String.valueOf(100 + i))
                    .status(RoomStatus.AVAILABLE)
                    .roomType(standardRoom)
                    .build());
        }

        RoomTypeDto standardDto = RoomTypeDto.builder()
                .id(1L)
                .name("Standard Room")
                .capacity(2)
                .basePrice(new BigDecimal("100.00"))
                .build();

        when(roomRepository.findAvailableRoomsByDateRange(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(rooms);

        when(roomTypeMapper.toDto(standardRoom)).thenReturn(standardDto);

        // When
        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        // Should recommend 2 Standard Rooms (exactly as requested, capacity 4 for 4 guests)
        assertAll(() -> verify(bookingValidator).validateRoomOptionsRequestDto(requestDto),
                () -> assertThat(result).isNotNull(), () -> assertThat(result).hasSize(1),
                () -> assertThat(result.getFirst().getRoomType().getName()).isEqualTo("Standard Room"),
                () -> assertThat(result.getFirst().getAvailableCount()).isEqualTo(10),
                () -> assertThat(result.getFirst().getRecommendedCount()).isEqualTo(2),
                () -> assertThat(result.getFirst().getTotalPricePerNight()).isEqualTo(new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("createBooking should create booking with new guest and assign selected rooms")
    public void createBooking_whenStaffManuallyAssignsRooms_createsBookingAndAssignsThoseSpecificRooms() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(5);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john.doe@example.com");
        guestInfo.setPhone("+43 123 456 7890");

        RoomTypeSelectionDto selection1 = new RoomTypeSelectionDto(1L, 2);
        RoomTypeSelectionDto selection2 = new RoomTypeSelectionDto(2L, 1);

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();

        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(4);
        requestDto.setNumChildren(1);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(1L, 2L, 3L));
        requestDto.setCreatedManually(true);

        // Mock guest creation
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        Guest savedGuest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+43 123 456 7890")
                .build();
        when(guestRepository.save(any(Guest.class))).thenReturn(savedGuest);

        RoomType roomType1 = RoomType.builder().id(1L).name("Standard Room").capacity(2).build();
        RoomType roomType2 = RoomType.builder().id(2L).name("Deluxe Room").capacity(3).build();

        Room room101 = Room.builder().id(1L).roomNumber("101").floor(1).roomType(roomType1).build();
        Room room102 = Room.builder().id(2L).roomNumber("102").floor(1).roomType(roomType1).build();
        Room room103 = Room.builder().id(3L).roomNumber("103").floor(1).roomType(roomType2).build();

        when(roomRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(room102, room101, room103));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate))
                .thenReturn(List.of(room101, room102, room103));

        // Mock booking save
        Booking savedBooking = Booking.builder()
                .id(100L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(4)
                .numChildren(1)
                .status(BookingStatus.CONFIRMED)
                .guest(savedGuest)
                .rooms(new HashSet<>(List.of(room101, room102, room103)))
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        CreateOrUpdateBookingResponseDto response = bookingService.createBooking(requestDto);

        // Then
        assertAll(() -> assertThat(response).isNotNull(), () -> assertThat(response.getBookingId()).isEqualTo(100L),
                () -> assertThat(response.getGuestEmail()).isEqualTo("john.doe@example.com"),
                () -> assertThat(response.getStatus()).isEqualTo("CONFIRMED"),
                () -> assertThat(response.getAssignedRoomNumbers()).hasSize(3),
                () -> assertThat(response.getAssignedRoomNumbers()).containsExactly("101", "102", "103"),
                () -> verify(guestRepository, times(1))
                        .findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob),
                () -> verify(guestRepository, times(1)).save(any(Guest.class)),
                () -> verify(bookingRepository, times(1)).save(any(Booking.class)));
    }

    @Test
    @DisplayName("createBooking should use existing guest when email matches")
    public void createBooking_whenExistingGuest_usesExistingGuest() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        tuwien.taskinn.dto.request.GuestInfoDto guestInfo = new tuwien.taskinn.dto.request.GuestInfoDto();
        guestInfo.setFirstName("Jane");
        guestInfo.setLastName("Smith");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("new-email@example.com");
        guestInfo.setPhone("+43 999 888 777");

        tuwien.taskinn.dto.request.CreateOrUpdateBookingRequestDto requestDto =
                new tuwien.taskinn.dto.request.CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(10L));
        requestDto.setCreatedManually(true);

        // Mock existing guest
        Guest existingGuest = Guest.builder()
                .id(5L)
                .firstName("Jane")
                .lastName("Smith")
                .email("old-email@example.com")
                .phone("+43 111 222 3333")
                .build();
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("Jane", "Smith", dob))
                .thenReturn(Optional.of(existingGuest));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoomType roomType1 = RoomType.builder().id(1L).name("Standard Room").capacity(2).build();
        Room room201 = Room.builder().id(10L).roomNumber("201").floor(2).roomType(roomType1).build();
        when(roomRepository.findAllById(List.of(10L))).thenReturn(List.of(room201));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room201));

        // Mock booking save
        Booking savedBooking = Booking.builder()
                .id(200L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .guest(existingGuest)
                .rooms(new HashSet<>(List.of(room201)))
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        CreateOrUpdateBookingResponseDto response = bookingService.createBooking(requestDto);

        // Then
        assertAll(() -> assertThat(response).isNotNull(),
                () -> assertThat(response.getGuestEmail()).isEqualTo("new-email@example.com"),
                () -> verify(guestRepository, times(1))
                        .findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("Jane", "Smith", dob),
                () -> verify(guestRepository, times(1)).save(existingGuest),
                () -> assertThat(existingGuest.getEmail()).isEqualTo("new-email@example.com"),
                () -> assertThat(existingGuest.getPhone()).isEqualTo("+43 999 888 777"));
    }

    @Test
    @DisplayName("createBooking should throw exception when checkout date is before checkin date")
    public void createBooking_whenInvalidDates_throwsException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(3); // Earlier than check-in

        tuwien.taskinn.dto.request.GuestInfoDto guestInfo = new tuwien.taskinn.dto.request.GuestInfoDto();
        guestInfo.setEmail("test@example.com");

        tuwien.taskinn.dto.request.RoomTypeSelectionDto selection =
                new tuwien.taskinn.dto.request.RoomTypeSelectionDto(1L, 1);

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(1L));
        requestDto.setCreatedManually(true);

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(requestDto));

        assertThat(exception.getMessage()).contains("Check-out date must be after check-in date");
    }

    @Test
    @DisplayName("createBooking should throw exception when a selected room does not exist")
    public void createBooking_whenRoomNotFound_throwsException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john@example.com");

        tuwien.taskinn.dto.request.RoomTypeSelectionDto selection =
                new tuwien.taskinn.dto.request.RoomTypeSelectionDto(999L, 1); // Non-existent room type

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(999L));
        requestDto.setCreatedManually(true);

        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class)))
                .thenReturn(Guest.builder().id(1L).email("john@example.com").build());
        when(roomRepository.findAllById(List.of(999L))).thenReturn(Collections.emptyList());

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(requestDto));

        assertThat(exception.getMessage()).contains("Some rooms were not found");
    }

    @Test
    @DisplayName("createBooking should throw exception when a selected room is unavailable")
    public void createBooking_whenRoomUnavailable_throwsException() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        tuwien.taskinn.dto.request.GuestInfoDto guestInfo = new tuwien.taskinn.dto.request.GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john@example.com");

        tuwien.taskinn.dto.request.RoomTypeSelectionDto selection =
                new tuwien.taskinn.dto.request.RoomTypeSelectionDto(1L, 5); // Request 5 rooms

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(10);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(1L, 2L));
        requestDto.setCreatedManually(true);

        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class)))
                .thenReturn(Guest.builder().id(1L).email("john@example.com").build());

        RoomType roomType1 = RoomType.builder().id(1L).name("Standard Room").capacity(2).build();
        Room room101 = Room.builder().id(1L).roomNumber("101").roomType(roomType1).build();
        Room room102 = Room.builder().id(2L).roomNumber("102").roomType(roomType1).build();
        when(roomRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(room101, room102));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room101));

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(requestDto));

        assertThat(exception.getMessage()).contains("Room 102 is not available for the selected dates");
    }

    @Test
    @DisplayName("createBooking should return assigned rooms sorted by room number")
    public void createBooking_whenMultipleRoomsRequested_returnsSortedRooms() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Family");
        guestInfo.setLastName("Test");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("family@example.com");

        tuwien.taskinn.dto.request.RoomTypeSelectionDto selection =
                new tuwien.taskinn.dto.request.RoomTypeSelectionDto(1L, 3);

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(6);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(1L, 2L, 3L));
        requestDto.setCreatedManually(true);

        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("Family", "Test", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class)))
                .thenReturn(Guest.builder().id(1L).email("family@example.com").build());

        RoomType roomType1 = RoomType.builder().id(1L).name("Standard Room").capacity(2).build();
        // Rooms in different floors - order returned by repository is unsorted
        Room room101 = Room.builder().id(1L).roomNumber("101").floor(1).roomType(roomType1).build();
        Room room102 = Room.builder().id(2L).roomNumber("102").floor(1).roomType(roomType1).build();
        Room room103 = Room.builder().id(3L).roomNumber("103").floor(1).roomType(roomType1).build();
        Room room201 = Room.builder().id(4L).roomNumber("201").floor(2).roomType(roomType1).build();
        Room room301 = Room.builder().id(5L).roomNumber("301").floor(3).roomType(roomType1).build();

        when(roomRepository.findAllById(List.of(1L, 2L, 3L))).thenReturn(List.of(room103, room101, room102));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate))
                .thenReturn(List.of(room201, room101, room301, room102, room103));

        Booking savedBooking = Booking.builder()
                .id(300L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .status(BookingStatus.CONFIRMED)
                .rooms(new HashSet<>(List.of(room103, room101, room102)))
                .build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        CreateOrUpdateBookingResponseDto response = bookingService.createBooking(requestDto);

        // Then
        assertAll(() -> assertThat(response).isNotNull(),
                () -> assertThat(response.getAssignedRoomNumbers()).hasSize(3),
                () -> assertThat(response.getAssignedRoomNumbers()).containsExactly("101", "102", "103"));
    }

    @Test
    @DisplayName("getRoomTypeOptions should filter out inactive rooms from the results")
    void getRoomTypeOptions_whenRoomsAreInactive_filtersThemOutOfOptions() {
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        RoomOptionsRequestDto requestDto = new RoomOptionsRequestDto(checkInDate, checkOutDate, 1, 1, 0);

        RoomType standardType =
                RoomType.builder().id(1L).name("Standard Room").capacity(2).basePrice(new BigDecimal("100.00")).build();

        Room activeRoom = Room.builder()
                .id(101L)
                .roomNumber("101")
                .status(RoomStatus.AVAILABLE)
                .active(true) // <-- This room is active
                .roomType(standardType)
                .build();

        Room inactiveRoom = Room.builder()
                .id(102L)
                .roomNumber("102")
                .status(RoomStatus.AVAILABLE)
                .active(false) // <-- THIS ROOM IS INACTIVE
                .roomType(standardType)
                .build();

        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(activeRoom));

        RoomTypeDto standardDto = RoomTypeDto.builder().id(1L).name("Standard Room").build();
        when(roomTypeMapper.toDto(standardType)).thenReturn(standardDto);

        List<RoomTypeOptionDto> result = bookingService.getRoomTypeOptions(requestDto);

        assertAll("Verify that only active rooms are counted", () -> assertThat(result).hasSize(1), () -> {
            RoomTypeOptionDto standardOption = result.get(0);
            assertThat(standardOption.getAvailableCount()).isEqualTo(1);
            assertThat(standardOption.getRoomType().getName()).isEqualTo("Standard Room");
        }, () -> verify(roomRepository, times(1)).findAvailableRoomsByDateRange(checkInDate, checkOutDate));
    }

    @Test
    @DisplayName("checkOutBooking should throw IllegalArgumentException when booking does not exist")
    void givenNonExistingBooking_whenCheckOutBooking_thenThrowsEntityNotFoundException() {
        // Given
        Long bookingId = 999L;
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.checkOutBooking(bookingId));

        // Then
        assertAll(() -> assertEquals("Booking not found", exception.getMessage()),
                () -> verify(bookingRepository, times(1)).findById(bookingId),
                () -> verify(bookingRepository, never()).save(any()));
    }

    @Test
    @DisplayName("checkOutBooking should set status to CHECKED_OUT for a checked-in booking")
    void givenCheckedInBooking_whenCheckOutBooking_thenStatusUpdatedToCheckedOut() {
        // Given
        Long bookingId = 1L;
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CHECKED_IN)
                .checkInDate(LocalDate.now())
                .rooms(new HashSet<>())
                .guest(Guest.builder().email("guest@email.com").build())
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        bookingService.checkOutBooking(bookingId);

        // Then
        assertAll(() -> assertEquals(BookingStatus.CHECKED_OUT, booking.getStatus()),
                () -> verify(bookingRepository, times(1)).findById(bookingId),
                () -> verify(bookingRepository, times(1)).save(booking));
    }

    @Test
    @DisplayName("updateBooking should throw exception when booking does not exist")
    void updateBooking_whenBookingNotFound_throwsException() {
        // Given
        Long bookingId = 999L;
        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        // When & Then
        NoSuchElementException exception =
                assertThrows(NoSuchElementException.class, () -> bookingService.updateBooking(bookingId, requestDto));

        assertAll(() -> assertThat(exception.getMessage()).contains("Booking not found with id: 999"),
                () -> verify(bookingRepository, times(1)).findById(bookingId),
                () -> verify(bookingRepository, never()).save(any()));
    }

    @Test
    @DisplayName("updateBooking should throw exception when booking status is not CONFIRMED")
    void updateBooking_whenStatusNotConfirmed_throwsException() {
        // Given
        Long bookingId = 1L;
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CHECKED_IN) // Already checked in
                .createdManually(true)
                .build();

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.updateBooking(bookingId, requestDto));

        assertAll(
                () -> assertThat(exception.getMessage()).contains("Only CONFIRMED bookings can be updated")
                        .contains("Current status: CHECKED_IN"),
                () -> verify(bookingRepository, times(1)).findById(bookingId),
                () -> verify(bookingRepository, never()).save(any()));
    }

    @Test
    @DisplayName("updateBooking should throw exception when check-out date is before check-in date")
    void updateBooking_whenInvalidDates_throwsException() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(3); // Earlier than check-in

        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.CONFIRMED).createdManually(true).build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(LocalDate.of(1990, 1, 1));
        guestInfo.setEmail("john@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setGuestInfo(guestInfo);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.updateBooking(bookingId, requestDto));

        assertThat(exception.getMessage()).contains("Check-out date must be after check-in date");
    }

    @Test
    @DisplayName("updateBooking should throw exception when guest is under 18 years old")
    void updateBooking_whenGuestUnder18_throwsException() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate underageDob = LocalDate.now().minusYears(17); // 17 years old

        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.CONFIRMED).createdManually(true).build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Young");
        guestInfo.setLastName("Guest");
        guestInfo.setDateOfBirth(underageDob);
        guestInfo.setEmail("young@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setGuestInfo(guestInfo);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.updateBooking(bookingId, requestDto));

        assertThat(exception.getMessage()).contains("Booking guest must be at least 18 years old");
    }

    @Test
    @DisplayName("updateBooking should throw exception when room type selections are provided")
    void updateBooking_whenRoomTypeSelections_throwsException() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        Booking booking = Booking.builder().id(bookingId).status(BookingStatus.CONFIRMED).createdManually(true).build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john@example.com");

        tuwien.taskinn.dto.request.RoomTypeSelectionDto selection =
                new tuwien.taskinn.dto.request.RoomTypeSelectionDto(1L, 1);

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomTypeSelections(List.of(selection)); // Not supported for updates

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class)))
                .thenReturn(Guest.builder().id(1L).email("john@example.com").build());

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.updateBooking(bookingId, requestDto));

        assertThat(exception.getMessage()).contains("Room type selections are not supported for booking updates")
                .contains("Please specify room IDs");
    }

    @Test
    @DisplayName("updateBooking should throw exception when room is not available")
    void updateBooking_whenRoomNotAvailable_throwsException() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        Room existingRoom = Room.builder().id(101L).roomNumber("101").build();
        Room requestedRoom = Room.builder().id(102L).roomNumber("102").build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .rooms(new HashSet<>(Set.of(existingRoom)))
                .build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john@example.com");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(102L)); // Room 102 is not available

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class)))
                .thenReturn(Guest.builder().id(1L).email("john@example.com").build());
        when(roomRepository.findAllById(List.of(102L))).thenReturn(List.of(requestedRoom));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate))
                .thenReturn(Collections.emptyList()); // No rooms available

        // When & Then
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> bookingService.updateBooking(bookingId, requestDto));

        assertThat(exception.getMessage()).contains("Room 102 is not available for the selected dates");
    }

    @Test
    @DisplayName("updateBooking should successfully update booking with new room selection")
    void updateBooking_whenValidData_updatesBookingSuccessfully() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        Room existingRoom = Room.builder().id(101L).roomNumber("101").build();
        Room newRoom = Room.builder().id(102L).roomNumber("102").build();

        Guest existingGuest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("old@example.com")
                .phone("111-222-3333")
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(7))
                .numAdults(2)
                .numChildren(0)
                .guest(existingGuest)
                .rooms(new HashSet<>(Set.of(existingRoom)))
                .build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("new@example.com");
        guestInfo.setPhone("999-888-7777");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(3);
        requestDto.setNumChildren(1);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(102L));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.of(existingGuest));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.findAllById(List.of(102L))).thenReturn(List.of(newRoom));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(newRoom));

        // When
        bookingService.updateBooking(bookingId, requestDto);

        // Then
        assertAll(() -> assertThat(booking.getCheckInDate()).isEqualTo(checkInDate),
                () -> assertThat(booking.getCheckOutDate()).isEqualTo(checkOutDate),
                () -> assertThat(booking.getNumAdults()).isEqualTo(3),
                () -> assertThat(booking.getNumChildren()).isEqualTo(1),
                () -> assertThat(booking.getGuest().getEmail()).isEqualTo("new@example.com"),
                () -> assertThat(booking.getGuest().getPhone()).isEqualTo("999-888-7777"),
                () -> assertThat(booking.getRooms()).contains(newRoom),
                () -> verify(bookingRepository, times(1)).save(booking),
                () -> verify(emailService, times(1)).sendBookingConfirmationEmail("new@example.com", booking));
    }

    @Test
    @DisplayName("updateBooking should allow keeping the same rooms from the current booking")
    void updateBooking_whenKeepingSameRooms_allowsUpdate() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        Room room101 = Room.builder().id(101L).roomNumber("101").build();

        Guest existingGuest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("john@example.com")
                .phone("123-456-7890")
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(7))
                .numAdults(2)
                .numChildren(0)
                .guest(existingGuest)
                .rooms(new HashSet<>(Set.of(room101)))
                .build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john@example.com");
        guestInfo.setPhone("123-456-7890");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L)); // Same room as before

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.of(existingGuest));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room101));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate))
                .thenReturn(Collections.emptyList()); // Room 101 is booked by another booking, but should still work
                                                      // because we exclude the current booking

        // When
        bookingService.updateBooking(bookingId, requestDto);

        // Then
        assertAll(() -> assertThat(booking.getCheckInDate()).isEqualTo(checkInDate),
                () -> assertThat(booking.getCheckOutDate()).isEqualTo(checkOutDate),
                () -> assertThat(booking.getRooms()).contains(room101),
                () -> verify(bookingRepository, times(1)).save(booking));
    }

    @Test
    @DisplayName("updateBooking should create new guest when guest doesn't exist")
    void updateBooking_whenNewGuest_createsGuest() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        LocalDate checkOutDate = LocalDate.now().plusDays(3);
        LocalDate dob = LocalDate.of(1985, 5, 15);

        Room room101 = Room.builder().id(101L).roomNumber("101").build();

        Guest oldGuest = Guest.builder().id(1L).firstName("Old").lastName("Guest").email("old@example.com").build();

        Guest newGuest = Guest.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(dob)
                .email("jane@example.com")
                .phone("555-1234")
                .build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .createdManually(true)
                .checkInDate(LocalDate.now().plusDays(5))
                .checkOutDate(LocalDate.now().plusDays(7))
                .numAdults(2)
                .numChildren(0)
                .guest(oldGuest)
                .rooms(new HashSet<>(Set.of(room101)))
                .build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("Jane");
        guestInfo.setLastName("Smith");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("jane@example.com");
        guestInfo.setPhone("555-1234");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("Jane", "Smith", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenReturn(newGuest);
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room101));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room101));

        // When
        bookingService.updateBooking(bookingId, requestDto);

        // Then
        assertAll(() -> assertThat(booking.getGuest()).isEqualTo(newGuest),
                () -> verify(guestRepository, times(1)).save(any(Guest.class)),
                () -> verify(bookingRepository, times(1)).save(booking));
    }

    @Test
    @DisplayName("Create booking with early check-in should set early check-in flag")
    void createBooking_withEarlyCheckIn_shouldSetEarlyCheckInFlag() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john.doe@example.com");
        guestInfo.setPhone("+43 123 456 789");

        Guest guest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("john.doe@example.com")
                .phone("+43 123 456 789")
                .build();

        RoomType roomType =
                RoomType.builder().id(1L).name("Standard").capacity(2).basePrice(new BigDecimal("100.00")).build();

        Room room = Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(roomType).build();

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));
        requestDto.setEarlyCheckIn(true);
        requestDto.setLateCheckOut(false);

        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room));

        Booking savedBooking = Booking.builder()
                .id(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .guest(guest)
                .rooms(new HashSet<>(List.of(room)))
                .earlyCheckIn(true)
                .lateCheckOut(false)
                .payments(new HashSet<>())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        CreateOrUpdateBookingResponseDto result = bookingService.createBooking(requestDto);

        // Then
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getEarlyCheckIn()).isTrue(),
                () -> assertThat(result.getLateCheckOut()).isFalse(),
                () -> verify(bookingRepository, times(1)).save(any(Booking.class)));
    }

    @Test
    @DisplayName("Create booking with late check-out should set late check-out flag")
    void createBooking_withLateCheckOut_shouldSetLateCheckOutFlag() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john.doe@example.com");
        guestInfo.setPhone("+43 123 456 789");

        Guest guest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("john.doe@example.com")
                .phone("+43 123 456 789")
                .build();

        RoomType roomType =
                RoomType.builder().id(1L).name("Standard").capacity(2).basePrice(new BigDecimal("100.00")).build();

        Room room = Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(roomType).build();

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));
        requestDto.setEarlyCheckIn(false);
        requestDto.setLateCheckOut(true);

        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room));

        Booking savedBooking = Booking.builder()
                .id(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .guest(guest)
                .rooms(new HashSet<>(List.of(room)))
                .earlyCheckIn(false)
                .lateCheckOut(true)
                .payments(new HashSet<>())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        CreateOrUpdateBookingResponseDto result = bookingService.createBooking(requestDto);

        // Then
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getEarlyCheckIn()).isFalse(),
                () -> assertThat(result.getLateCheckOut()).isTrue());
    }

    @Test
    @DisplayName("Create booking with both early check-in and late check-out should set both flags")
    void createBooking_withBothServices_shouldSetBothFlags() {
        // Given
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john.doe@example.com");
        guestInfo.setPhone("+43 123 456 789");

        Guest guest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("john.doe@example.com")
                .phone("+43 123 456 789")
                .build();

        RoomType roomType =
                RoomType.builder().id(1L).name("Standard").capacity(2).basePrice(new BigDecimal("100.00")).build();

        Room room = Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(roomType).build();

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));
        requestDto.setEarlyCheckIn(true);
        requestDto.setLateCheckOut(true);

        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room));

        Booking savedBooking = Booking.builder()
                .id(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .guest(guest)
                .rooms(new HashSet<>(List.of(room)))
                .earlyCheckIn(true)
                .lateCheckOut(true)
                .payments(new HashSet<>())
                .build();

        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        // When
        CreateOrUpdateBookingResponseDto result = bookingService.createBooking(requestDto);

        // Then
        assertAll(() -> assertThat(result).isNotNull(), () -> assertThat(result.getEarlyCheckIn()).isTrue(),
                () -> assertThat(result.getLateCheckOut()).isTrue());
    }

    @Test
    @DisplayName("Update booking to add early check-in should set early check-in flag")
    void updateBooking_addEarlyCheckIn_shouldSetEarlyCheckInFlag() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        Guest guest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("john.doe@example.com")
                .phone("+43 123 456 789")
                .build();

        RoomType roomType =
                RoomType.builder().id(1L).name("Standard").capacity(2).basePrice(new BigDecimal("100.00")).build();

        Room room = Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(roomType).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .guest(guest)
                .rooms(new HashSet<>(List.of(room)))
                .earlyCheckIn(false)
                .lateCheckOut(false)
                .payments(new HashSet<>())
                .build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john.doe@example.com");
        guestInfo.setPhone("+43 123 456 789");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));
        requestDto.setEarlyCheckIn(true);
        requestDto.setLateCheckOut(false);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.of(guest));
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(guestRepository.existsByEmailAndIdNot("john.doe@example.com", 1L)).thenReturn(false);
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room));

        // When
        bookingService.updateBooking(bookingId, requestDto);

        // Then
        assertAll(() -> assertThat(booking.getEarlyCheckIn()).isTrue(),
                () -> assertThat(booking.getLateCheckOut()).isFalse(),
                () -> verify(bookingRepository, times(1)).save(booking));
    }

    @Test
    @DisplayName("Update booking to remove early check-in should clear early check-in flag")
    void updateBooking_removeEarlyCheckIn_shouldClearEarlyCheckInFlag() {
        // Given
        Long bookingId = 1L;
        LocalDate checkInDate = LocalDate.now().plusDays(5);
        LocalDate checkOutDate = LocalDate.now().plusDays(10);
        LocalDate dob = LocalDate.of(1990, 1, 1);

        Guest guest = Guest.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(dob)
                .email("john.doe@example.com")
                .phone("+43 123 456 789")
                .build();

        RoomType roomType =
                RoomType.builder().id(1L).name("Standard").capacity(2).basePrice(new BigDecimal("100.00")).build();

        Room room = Room.builder().id(101L).roomNumber("101").status(RoomStatus.AVAILABLE).roomType(roomType).build();

        Booking booking = Booking.builder()
                .id(bookingId)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .numAdults(2)
                .numChildren(0)
                .status(BookingStatus.CONFIRMED)
                .guest(guest)
                .rooms(new HashSet<>(List.of(room)))
                .earlyCheckIn(true)
                .lateCheckOut(false)
                .payments(new HashSet<>())
                .build();

        GuestInfoDto guestInfo = new GuestInfoDto();
        guestInfo.setFirstName("John");
        guestInfo.setLastName("Doe");
        guestInfo.setDateOfBirth(dob);
        guestInfo.setEmail("john.doe@example.com");
        guestInfo.setPhone("+43 123 456 789");

        CreateOrUpdateBookingRequestDto requestDto = new CreateOrUpdateBookingRequestDto();
        requestDto.setCheckInDate(checkInDate);
        requestDto.setCheckOutDate(checkOutDate);
        requestDto.setNumAdults(2);
        requestDto.setNumChildren(0);
        requestDto.setGuestInfo(guestInfo);
        requestDto.setRoomIds(List.of(101L));
        requestDto.setEarlyCheckIn(false);
        requestDto.setLateCheckOut(false);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(guestRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth("John", "Doe", dob))
                .thenReturn(Optional.of(guest));
        when(guestRepository.save(any(Guest.class))).thenReturn(guest);
        when(guestRepository.existsByEmailAndIdNot("john.doe@example.com", 1L)).thenReturn(false);
        when(roomRepository.findAllById(List.of(101L))).thenReturn(List.of(room));
        when(roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate)).thenReturn(List.of(room));

        // When
        bookingService.updateBooking(bookingId, requestDto);

        // Then
        assertAll(() -> assertThat(booking.getEarlyCheckIn()).isFalse(),
                () -> assertThat(booking.getLateCheckOut()).isFalse(),
                () -> verify(bookingRepository, times(1)).save(booking));
    }

    @Test
    void getSpecialBookingConstraints_whenConstrainedRoomsExist_returnIdsOfConstrainedRooms() {
        final List<Long> earlyIds = List.of(1L, 2L, 3L);
        final List<Long> lateIds = List.of(10L, 12L, 13L);

        when(bookingRepository.findAllEarlyCheckInRoomIdsOnDay(any())).thenReturn(earlyIds);
        when(bookingRepository.findAllLateCheckOutRoomIdsOnDay(any())).thenReturn(lateIds);

        final SpecialBookingConstraintsDto result = bookingService.getSpecialBookingConstraints(2025, 1, 1);

        assertAll(() -> assertThat(result.getEarlyCheckInRoomIds()).containsExactlyInAnyOrderElementsOf(earlyIds),
                () -> assertThat(result.getLateCheckOutRoomIds()).containsExactlyInAnyOrderElementsOf(lateIds));
    }
}
