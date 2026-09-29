package tuwien.taskinn.services.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import tuwien.taskinn.config.FileStorageConfig;
import tuwien.taskinn.dto.request.CreateOrUpdateBookingRequestDto;
import tuwien.taskinn.dto.request.RoomOptionsRequestDto;
import tuwien.taskinn.dto.request.RoomTypeSelectionDto;
import tuwien.taskinn.dto.request.GuestInfoDto;
import tuwien.taskinn.dto.response.*;
import tuwien.taskinn.entity.*;
import tuwien.taskinn.entity.Payment;
import tuwien.taskinn.enums.RoomStatus;
import tuwien.taskinn.events.BookingStateChangedEvent;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.mapper.BookingMapper;
import tuwien.taskinn.mapper.RoomTypeMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import tuwien.taskinn.enums.PaymentStatus;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.GuestRepository;
import tuwien.taskinn.repo.RoomTypeRepository;
import tuwien.taskinn.repo.RoomRepository;
import tuwien.taskinn.services.BookingService;
import tuwien.taskinn.services.EmailService;
import tuwien.taskinn.services.validator.BookingValidator;
import tuwien.taskinn.services.validator.CheckInValidator;
import tuwien.taskinn.util.EncryptionUtil;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingValidator bookingValidator;
    private final BookingRepository bookingRepository;
    private final GuestRepository guestRepository;
    private final CheckInValidator checkInValidator;
    private final EncryptionUtil encryptionUtil;
    private final FileStorageConfig fileStorageConfig;

    private final RoomRepository roomRepository;
    private final RoomTypeMapper roomTypeMapper;
    private final RoomTypeRepository roomTypeRepository;
    private final BookingMapper bookingMapper;

    private final EmailService emailService;
    private final ApplicationEventPublisher publisher;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    @Override
    public List<RoomTypeOptionDto> getRoomTypeOptions(RoomOptionsRequestDto roomOptionsRequestDto) {
        this.bookingValidator.validateRoomOptionsRequestDto(roomOptionsRequestDto);

        Integer totalGuests = roomOptionsRequestDto.getNumAdults() + roomOptionsRequestDto.getNumChildren();

        // Get all available rooms for the date range
        List<Room> availableRooms = roomRepository.findAvailableRoomsByDateRange(roomOptionsRequestDto.getCheckInDate(),
                roomOptionsRequestDto.getCheckOutDate());

        // Find the best room combination for this booking
        List<Room> bestRoomSelection = findBestAvailableRooms(roomOptionsRequestDto, availableRooms, totalGuests);

        // Aggregate by room type and effective price
        Map<RoomTypePriceKey, RoomTypeAggregation> typeAggregationMap = new HashMap<>();

        // First, count all available rooms by type
        for (Room room : availableRooms) {
            RoomType roomType = room.getRoomType();
            BigDecimal pricePerNight = room.getEffectiveBasePrice();
            RoomTypePriceKey key = new RoomTypePriceKey(roomType.getId(), pricePerNight);
            typeAggregationMap.computeIfAbsent(key, id -> new RoomTypeAggregation(roomType, key.pricePerNight))
                    .incrementAvailableCount();
        }

        // Second, count recommended rooms by type from best selection
        for (Room room : bestRoomSelection) {
            RoomType roomType = room.getRoomType();
            BigDecimal pricePerNight = room.getEffectiveBasePrice();
            RoomTypePriceKey key = new RoomTypePriceKey(roomType.getId(), pricePerNight);
            typeAggregationMap.computeIfAbsent(key, id -> new RoomTypeAggregation(roomType, key.pricePerNight))
                    .incrementRecommendedCount();
        }

        // Convert to DTOs - sort by room type base price, then by room type, then by option price
        return typeAggregationMap.values()
                .stream()
                .filter(agg -> agg.availableCount > 0) // Return all available room types
                .sorted(Comparator
                        .comparing((RoomTypeAggregation agg) -> agg.roomType.getBasePrice(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(agg -> agg.roomType.getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(agg -> agg.pricePerNight, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(RoomTypeAggregation::getRecommendedCount, Comparator.reverseOrder())
                        .thenComparing(agg -> agg.roomType.getId(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(agg -> RoomTypeOptionDto.builder()
                        .roomType(roomTypeMapper.toDto(agg.roomType))
                        .pricePerNight(agg.pricePerNight)
                        .availableCount(agg.availableCount)
                        .recommendedCount(agg.recommendedCount)
                        .totalPricePerNight(agg.recommendedTotalPrice)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Finds single rooms that can accommodate all guests
     */
    private List<Room> findSingleRoomOptions(List<Room> availableRooms, Integer totalGuests) {
        return availableRooms.stream()
                .filter(room -> room.getEffectiveCapacity() >= totalGuests)
                .collect(Collectors.toList());
    }

    @Transactional
    public void checkOutBooking(Long bookingId) {
        // Check if booking exists
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Check current status
        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Booking must be CHECKED_IN to be checked out");
        }

        // Set status to CHECKED_OUT
        booking.setStatus(BookingStatus.CHECKED_OUT);
        bookingRepository.save(booking);

        // Set all rooms to DIRTY
        List<Room> roomsToUpdate = new ArrayList<>(booking.getRooms());
        for (Room room : roomsToUpdate) {
            room.setStatus(RoomStatus.DIRTY);
        }
        roomRepository.saveAll(roomsToUpdate);

        this.emailService.sendInvoiceEmail(booking.getGuest().getEmail(), bookingMapper.toInvoicePreviewDto(booking));

        this.publisher.publishEvent(new BookingStateChangedEvent(booking.getId(), booking.getStatus()));
    }

    @Override
    public DetailedBookingResponseDto getBookingDetails(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found with id: " + bookingId));

        return bookingMapper.toDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSummaryResponseDto> findAllBookings() {
        return bookingRepository.findAllProjectedBy();
    }

    @Transactional(readOnly = true)
    @Override
    public List<RoomForCheckinResponseDto> getRoomsForCheckinByBookingId(Long bookingId) {
        checkInValidator.validateBookingStatusForCheckin(bookingId);
        return bookingRepository.findRoomsByBookingId(bookingId);
    }

    @Transactional
    @Override
    public void checkInBooking(Long bookingId, List<CheckInGuestDto> guestDtos, Map<String, MultipartFile> files) {
        // STEP 1: Validate booking, guests and files
        checkInValidator.validateCheckin(bookingId, guestDtos, files);

        // STEP 2: Find booking and set status to CHECKED_IN
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        booking.setStatus(BookingStatus.CHECKED_IN);

        // STEP 3: Set all rooms to OCCUPIED
        List<Long> roomIds = guestDtos.stream().map(CheckInGuestDto::getRoomId).distinct().toList();
        List<Room> roomsToUpdate = roomRepository.findAllById(roomIds);
        for (Room room : roomsToUpdate) {
            room.setStatus(RoomStatus.OCCUPIED);
        }
        roomRepository.saveAll(roomsToUpdate);

        // STEP 4.1: Create ids folder
        Path idsFolder = Path.of(fileStorageConfig.getStorageDir());
        idsFolder = idsFolder.resolve("ids");
        if (!Files.exists(idsFolder)) {
            try {
                Files.createDirectories(idsFolder);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create ids folder", e);
            }
        }

        // STEP 4.2: Encrypt images
        Set<Guest> guestsForThisBooking = new HashSet<>();
        for (int i = 0; i < guestDtos.size(); i++) {
            CheckInGuestDto guestDto = guestDtos.get(i);
            String fileKey = "guestFiles_" + i;
            MultipartFile file = files.get(fileKey);
            byte[] imageBytes;
            if (file != null && !file.isEmpty()) {
                try {
                    imageBytes = file.getBytes();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read file for guest " + i, e);
                }
            } else {
                throw new BusinessValidationException("idImage",
                        "Guest " + (i + 1) + " (" + guestDto.getFirstName() + "): ID image is required");
            }
            byte[] encryptedImage = encryptionUtil.encrypt(imageBytes);

            // STEP 4.3: Save encrypted image to ids folder
            String filename = "booking" + bookingId + "_guest" + (i + 1) + "_id.enc";
            Path filePath = idsFolder.resolve(filename);
            try {
                Files.write(filePath, encryptedImage, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save ID image for guest " + (i + 1), e);
            }
            Path relativePath = idsFolder.relativize(filePath);
            String relativePathStr = relativePath.toString().replace("\\", "/");

            Guest existingGuestByName = guestRepository
                    .findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(guestDto.getFirstName(),
                            guestDto.getLastName(), guestDto.getDateOfBirth())
                    .orElse(null);
            Guest existingGuestByEmail = guestRepository.findByEmail(guestDto.getEmail()).orElse(null);

            // STEP 5.1: If guest exists by name and date of birth:
            // If email is used by another guest, throw an error
            // Otherwise, update the guest
            if (existingGuestByName != null) {
                if (existingGuestByEmail != null
                        && guestRepository.existsByEmailAndIdNot(guestDto.getEmail(), existingGuestByName.getId())) {
                    throw new BusinessValidationException("email_" + guestDto.getEmail(),
                            "Email is already used by another guest.");
                }
                existingGuestByName.setEmail(guestDto.getEmail());
                existingGuestByName.setPhone(guestDto.getPhoneNumber());
                existingGuestByName.setIdImageUrl(relativePathStr);
                guestRepository.save(existingGuestByName);
                guestsForThisBooking.add(existingGuestByName);
            } else {
                // STEP 5.2: If guest does not exist by name and date of birth:
                // If email is used, throw an error
                // Otherwise, create a new guest
                if (guestRepository.existsByEmail(guestDto.getEmail())) {
                    throw new BusinessValidationException("email_" + guestDto.getEmail(),
                            "Email is already used by another guest.");
                }
                Guest newGuest = Guest.builder()
                        .firstName(guestDto.getFirstName())
                        .lastName(guestDto.getLastName())
                        .dateOfBirth(guestDto.getDateOfBirth())
                        .email(guestDto.getEmail())
                        .phone(guestDto.getPhoneNumber())
                        .idImageUrl(relativePathStr)
                        .build();
                guestRepository.save(newGuest);
                guestsForThisBooking.add(newGuest);
            }
        }
        booking.setCheckedInGuests(guestsForThisBooking);
        bookingRepository.save(booking);

        this.publisher.publishEvent(new BookingStateChangedEvent(booking.getId(), booking.getStatus()));
    }

    // Invoked by Staff, Creates a new booking with the status CONFIRMED
    @Override
    @Transactional
    public CreateOrUpdateBookingResponseDto createBooking(CreateOrUpdateBookingRequestDto requestDto) {
        // Validate dates and guest
        validateCheckInOutDates(requestDto.getCheckInDate(), requestDto.getCheckOutDate());
        validateGuestAge(requestDto.getGuestInfo().getDateOfBirth());
        Guest guest = validateAndGetGuest(requestDto.getGuestInfo());

        if (requestDto.getRoomIds() == null || requestDto.getRoomIds().isEmpty()) {
            throw new IllegalArgumentException("Room IDs are required for manual bookings");
        }

        List<Room> selectedRooms;
        int totalGuests = requestDto.getNumAdults() + requestDto.getNumChildren();

        selectedRooms = validateAndSelectRoomsByIds(requestDto.getRoomIds(), requestDto.getCheckInDate(),
                requestDto.getCheckOutDate(), totalGuests, null);

        // Create booking
        Booking booking = Booking.builder()
                .checkInDate(requestDto.getCheckInDate())
                .checkOutDate(requestDto.getCheckOutDate())
                .numAdults(requestDto.getNumAdults())
                .numChildren(requestDto.getNumChildren())
                .status(BookingStatus.CONFIRMED)
                .createdManually(Boolean.TRUE.equals(requestDto.getCreatedManually()))
                .earlyCheckIn(Boolean.TRUE.equals(requestDto.getEarlyCheckIn()))
                .lateCheckOut(Boolean.TRUE.equals(requestDto.getLateCheckOut()))
                .guest(guest)
                .rooms(new HashSet<>(selectedRooms))
                .payments(new HashSet<>())
                .build();
        booking = bookingRepository.save(booking);

        this.emailService.sendBookingConfirmationEmail(requestDto.getGuestInfo().getEmail(), booking);

        // Build response
        List<String> roomNumbers =
                selectedRooms.stream().map(Room::getRoomNumber).sorted().collect(Collectors.toList());

        return CreateOrUpdateBookingResponseDto.builder()
                .bookingId(booking.getId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .assignedRoomNumbers(roomNumbers)
                .status(booking.getStatus().name())
                .guestEmail(guest.getEmail())
                .earlyCheckIn(booking.getEarlyCheckIn())
                .lateCheckOut(booking.getLateCheckOut())
                .build();
    }

    /**
     * Internal helper class to aggregate room data by type
     */
    private static final class RoomTypePriceKey {
        private final Long roomTypeId;
        private final BigDecimal pricePerNight;

        private RoomTypePriceKey(Long roomTypeId, BigDecimal pricePerNight) {
            this.roomTypeId = roomTypeId;
            this.pricePerNight = normalizePrice(pricePerNight);
        }

        private static BigDecimal normalizePrice(BigDecimal pricePerNight) {
            return pricePerNight == null ? null : pricePerNight.setScale(2, RoundingMode.UP);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RoomTypePriceKey that = (RoomTypePriceKey) o;
            return Objects.equals(roomTypeId, that.roomTypeId) && Objects.equals(pricePerNight, that.pricePerNight);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomTypeId, pricePerNight);
        }
    }

    private static class RoomTypeAggregation {
        private final RoomType roomType;
        private final BigDecimal pricePerNight;
        private BigDecimal recommendedTotalPrice = BigDecimal.ZERO;
        private int availableCount = 0;
        private int recommendedCount = 0;

        RoomTypeAggregation(RoomType roomType, BigDecimal pricePerNight) {
            this.roomType = roomType;
            this.pricePerNight = pricePerNight;
        }

        void incrementAvailableCount() {
            this.availableCount++;
        }

        void incrementRecommendedCount() {
            this.recommendedCount++;
            if (this.pricePerNight != null) {
                this.recommendedTotalPrice = this.recommendedTotalPrice.add(this.pricePerNight);
            }
        }

        int getRecommendedCount() {
            return recommendedCount;
        }
    }

    @Override
    @Transactional
    public void updateBooking(Long bookingId, CreateOrUpdateBookingRequestDto requestDto) {
        // Find existing booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found with id: " + bookingId));

        // Validate booking can be updated (only CONFIRMED bookings can be updated)
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "Only CONFIRMED bookings can be updated. Current status: " + booking.getStatus());
        }

        // Validate check-in/check-out dates
        validateCheckInOutDates(requestDto.getCheckInDate(), requestDto.getCheckOutDate());

        // Validate guest age
        validateGuestAge(requestDto.getGuestInfo().getDateOfBirth());

        // Validate and get/create guest
        Guest guest = validateAndGetGuest(requestDto.getGuestInfo());

        // Validate that either roomTypeSelections or roomIds is provided, but not both
        boolean hasRoomTypeSelections =
                requestDto.getRoomTypeSelections() != null && !requestDto.getRoomTypeSelections().isEmpty();
        boolean hasRoomIds = requestDto.getRoomIds() != null && !requestDto.getRoomIds().isEmpty();

        if (hasRoomTypeSelections == hasRoomIds) {
            throw new IllegalArgumentException("Either roomTypeSelections or roomIds must be provided, but not both");
        }

        List<Room> selectedRooms;
        int totalGuests = requestDto.getNumAdults() + requestDto.getNumChildren();

        if (hasRoomIds) {
            // Validate and select rooms, excluding current booking from availability check
            selectedRooms = validateAndSelectRoomsByIds(requestDto.getRoomIds(), requestDto.getCheckInDate(),
                    requestDto.getCheckOutDate(), totalGuests, bookingId);
        } else {
            // Staff booking with room type selections is not supported for updates
            throw new IllegalArgumentException(
                    "Room type selections are not supported for booking updates. Please specify room IDs.");
        }

        // Update booking fields
        booking.setCheckInDate(requestDto.getCheckInDate());
        booking.setCheckOutDate(requestDto.getCheckOutDate());
        booking.setNumAdults(requestDto.getNumAdults());
        booking.setNumChildren(requestDto.getNumChildren());
        booking.setGuest(guest);
        booking.setRooms(new HashSet<>(selectedRooms));

        booking.setEarlyCheckIn(requestDto.getEarlyCheckIn());
        booking.setLateCheckOut(requestDto.getLateCheckOut());

        bookingRepository.save(booking);

        // Send updated confirmation email
        this.emailService.sendBookingConfirmationEmail(guest.getEmail(), booking);
    }

    @Override
    public Long getTodaysCheckInCount() {
        LocalDate today = LocalDate.now();
        return bookingRepository.countBookingsNeedingCheckInOnDate(today);
    }

    @Override
    public Long getTodaysCheckOutCount() {
        LocalDate today = LocalDate.now();
        return bookingRepository.countBookingsNeedingCheckOutOnDate(today);
    }

    @Override
    public SpecialBookingConstraintsDto getSpecialBookingConstraints(Integer year, Integer month, Integer day) {
        validateYearMonthDay(year, month, day);
        final LocalDate date = LocalDate.of(year, month, day);

        final List<Long> early = bookingRepository.findAllEarlyCheckInRoomIdsOnDay(date);
        final List<Long> late = bookingRepository.findAllLateCheckOutRoomIdsOnDay(date);
        return SpecialBookingConstraintsDto.builder().earlyCheckInRoomIds(early).lateCheckOutRoomIds(late).build();
    }

    /**
     * Validates that the given date components represent a valid day
     */
    private void validateYearMonthDay(Integer year, Integer month, Integer day) {
        if (year == null) {
            throw new BusinessValidationException("year", "Year must not be null");
        } else if (year < 2014 || year > 9999) {
            throw new BusinessValidationException("year", "Year is not in the expected range 2014-9999");
        }

        if (month == null) {
            throw new BusinessValidationException("month", "Month must not be null");
        } else if (month <= 0 || month > 12) {
            throw new BusinessValidationException("month", "Month not in the expected range 1-12");
        }

        final int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        if (day == null) {
            throw new BusinessValidationException("day", "Day must not be null");
        } else if (day <= 0) {
            throw new BusinessValidationException("day", "Day must be greater than 0");
        } else if (day > daysInMonth) {
            throw new BusinessValidationException("day", "Day must not exceed days in month");
        }
    }

    /**
     * Validates that check-out date is after check-in date
     */
    private void validateCheckInOutDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
    }

    /**
     * Validates that guest is at least 18 years old
     */
    private void validateGuestAge(LocalDate dateOfBirth) {
        LocalDate today = LocalDate.now();
        long age = ChronoUnit.YEARS.between(dateOfBirth, today);
        if (age < 18) {
            throw new IllegalArgumentException("Booking guest must be at least 18 years old.");
        }
    }

    /**
     * Finds or creates a guest based on guest information
     */
    private Guest validateAndGetGuest(GuestInfoDto guestInfo) {
        Guest guest =
                guestRepository
                        .findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(guestInfo.getFirstName(),
                                guestInfo.getLastName(), guestInfo.getDateOfBirth())
                        .orElse(null);

        if (guest != null) {
            // Check if new email is used by a different guest
            if (guestInfo.getEmail() != null && !guestInfo.getEmail().isBlank()
                    && guestRepository.existsByEmailAndIdNot(guestInfo.getEmail(), guest.getId())) {
                throw new IllegalArgumentException("Email is already used by another guest.");
            }

            guest.setEmail(guestInfo.getEmail());
            guest.setPhone(guestInfo.getPhone());
            guest = guestRepository.save(guest);
        } else {
            // New guest: ensure email is not already taken
            if (guestInfo.getEmail() != null && !guestInfo.getEmail().isBlank()
                    && guestRepository.existsByEmail(guestInfo.getEmail())) {
                throw new IllegalArgumentException("Email is already used by another guest.");
            }

            guest = Guest.builder()
                    .firstName(guestInfo.getFirstName())
                    .lastName(guestInfo.getLastName())
                    .dateOfBirth(guestInfo.getDateOfBirth())
                    .email(guestInfo.getEmail())
                    .phone(guestInfo.getPhone())
                    .build();
            guest = guestRepository.save(guest);
        }

        return guest;
    }

    /**
     * Validates and selects rooms for manual booking by room IDs
     */
    private List<Room> validateAndSelectRoomsByIds(List<Long> roomIds, LocalDate checkInDate, LocalDate checkOutDate,
            int totalGuests, Long excludeBookingId) {
        int totalRequestedRooms = roomIds.size();

        // Validate total rooms <= total guests
        if (totalRequestedRooms > totalGuests) {
            throw new IllegalArgumentException(String.format(
                    "Cannot book %d rooms for only %d guests (adults + children).", totalRequestedRooms, totalGuests));
        }

        // Find rooms by IDs
        List<Room> selectedRooms = roomRepository.findAllById(roomIds);

        if (selectedRooms.size() != roomIds.size()) {
            throw new IllegalArgumentException("Some rooms were not found");
        }

        // Verify rooms are available for the date range
        List<Room> availableRooms = roomRepository.findAvailableRoomsByDateRange(checkInDate, checkOutDate);
        List<Long> availableRoomIds = availableRooms.stream().map(Room::getId).collect(Collectors.toList());

        // If updating a booking, add the current booking's rooms to available list
        if (excludeBookingId != null) {
            Booking existingBooking = bookingRepository.findById(excludeBookingId)
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found with id: " + excludeBookingId));
            Set<Long> currentBookingRoomIds =
                    existingBooking.getRooms().stream().map(Room::getId).collect(Collectors.toSet());
            availableRoomIds.addAll(currentBookingRoomIds);
        }

        // Validate each selected room is available
        for (Room room : selectedRooms) {
            if (!availableRoomIds.contains(room.getId())) {
                throw new IllegalArgumentException(
                        "Room " + room.getRoomNumber() + " is not available for the selected dates");
            }
        }

        return selectedRooms;
    }

    /**
     * Finds the best combination of available rooms that can accommodate all guests. This is the internal algorithm
     * that selects specific rooms from the available inventory.
     *
     * @param roomOptionsRequestDto The request parameters
     * @param availableRooms All available rooms for the date range
     * @param totalGuests Total number of guests to accommodate
     * @return List of specific Room objects representing the best selection
     */
    private List<Room> findBestAvailableRooms(RoomOptionsRequestDto roomOptionsRequestDto, List<Room> availableRooms,
            Integer totalGuests) {
        Integer requestedNumRooms = roomOptionsRequestDto.getNumRooms();

        // If multiple rooms are explicitly requested, try to fulfill that request first
        if (requestedNumRooms != null && requestedNumRooms > 1) {
            List<Room> exactRoomCountOptions =
                    findExactRoomCountOptions(availableRooms, totalGuests, requestedNumRooms);
            if (!exactRoomCountOptions.isEmpty()) {
                return exactRoomCountOptions;
            }
        }

        // First, try to find single rooms that can fit all guests
        List<Room> singleRoomOptions = findSingleRoomOptions(availableRooms, totalGuests);

        if (!singleRoomOptions.isEmpty()) {
            // Return single best-fit room (smallest capacity that fits)
            return singleRoomOptions.stream()
                    .min(Comparator.comparingInt(Room::getEffectiveCapacity))
                    .map(List::of)
                    .orElse(new ArrayList<>());
        }

        // If no single room fits, find combinations of multiple rooms
        return findBestRoomCombination(availableRooms, totalGuests);
    }

    /**
     * Finds exactly the requested number of rooms that can accommodate all guests. Prioritizes rooms that are close
     * together (by room number) and distributes guests evenly.
     */
    private List<Room> findExactRoomCountOptions(List<Room> availableRooms, Integer totalGuests,
            Integer requestedNumRooms) {
        // Check if we have enough available rooms
        if (availableRooms.size() < requestedNumRooms) {
            return new ArrayList<>();
        }

        // Calculate guests per room (ceiling division to ensure we have enough capacity)
        int guestsPerRoom = (int) Math.ceil((double) totalGuests / requestedNumRooms);

        // Find rooms that can accommodate at least guestsPerRoom
        List<Room> suitableRooms = availableRooms.stream()
                .filter(room -> room.getEffectiveCapacity() >= guestsPerRoom)
                .sorted(Comparator.comparing(this::extractNumericRoomNumber))
                .toList();

        // If we don't have enough suitable rooms, return empty list
        if (suitableRooms.size() < requestedNumRooms) {
            return new ArrayList<>();
        }

        // Select the first requestedNumRooms rooms (they're already sorted by proximity)
        List<Room> selectedRooms = suitableRooms.stream().limit(requestedNumRooms).collect(Collectors.toList());

        // Verify total capacity is sufficient
        int totalCapacity = selectedRooms.stream().mapToInt(r -> r.getEffectiveCapacity()).sum();

        if (totalCapacity < totalGuests) {
            return new ArrayList<>();
        }

        return selectedRooms;
    }

    /**
     * Finds the best combination of multiple rooms that can accommodate all guests, prioritizing rooms that are close
     * together (by room number)
     */
    private List<Room> findBestRoomCombination(List<Room> availableRooms, Integer totalGuests) {
        if (availableRooms.isEmpty()) {
            return new ArrayList<>();
        }

        // Sort rooms by room number to facilitate proximity-based selection
        List<Room> sortedRooms =
                availableRooms.stream().sorted(Comparator.comparing(this::extractNumericRoomNumber)).toList();

        List<Room> bestCombination = null;
        double bestProximityScore = Double.MAX_VALUE;

        // Try different combinations starting from each room
        for (int startIdx = 0; startIdx < sortedRooms.size(); startIdx++) {
            List<Room> combination = new ArrayList<>();
            int remainingCapacity = totalGuests;

            // Greedily select rooms starting from this position
            for (int i = startIdx; i < sortedRooms.size() && remainingCapacity > 0; i++) {
                Room room = sortedRooms.get(i);
                combination.add(room);
                remainingCapacity -= room.getEffectiveCapacity();
            }

            // Check if this combination can accommodate all guests
            int totalCapacity = combination.stream().mapToInt(Room::getEffectiveCapacity).sum();

            if (totalCapacity >= totalGuests) {
                // Calculate proximity score (lower is better)
                double proximityScore = calculateProximityScore(combination);

                // Update best combination if this one is better
                if (bestCombination == null || combination.size() < bestCombination.size()
                        || (combination.size() == bestCombination.size() && proximityScore < bestProximityScore)) {
                    bestCombination = new ArrayList<>(combination);
                    bestProximityScore = proximityScore;
                }
            }
        }

        return bestCombination != null ? bestCombination : new ArrayList<>();
    }

    /**
     * Calculates a proximity score for a list of rooms (lower is better) based on the variance of room numbers
     */
    private double calculateProximityScore(List<Room> rooms) {
        if (rooms.size() <= 1) {
            return 0.0;
        }

        List<Integer> roomNumbers = rooms.stream().map(this::extractNumericRoomNumber).toList();

        // Calculate average room number
        double average = roomNumbers.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        // Calculate variance (measure of spread)
        return roomNumbers.stream().mapToDouble(num -> Math.pow(num - average, 2)).average().orElse(0.0);
    }

    /**
     * Extracts numeric value from room number for sorting and proximity calculation Handles formats like "101", "A101",
     * "1-101", etc.
     */
    private Integer extractNumericRoomNumber(Room room) {
        String roomNumber = room.getRoomNumber();
        // Extract all digits from the room number
        String digits = roomNumber.replaceAll("\\D+", "");

        try {
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            // If parsing fails, return 0 (will be grouped together)
            return 0;
        }
    }

    /**
     * Optimizes the selection of rooms to ensure they are as close together as possible. This method finds the best
     * continuous sequence of rooms that minimizes the variance in room numbers.
     *
     * @param selectedRooms The initially selected rooms
     * @return Optimized list of rooms in close proximity
     */
    private List<Room> optimizeRoomProximity(List<Room> selectedRooms) {
        // Sort all rooms by room number
        List<Room> sortedRooms = selectedRooms.stream()
                .sorted(Comparator.comparing(this::extractNumericRoomNumber))
                .collect(Collectors.toList());

        // For small sets, just return sorted
        if (sortedRooms.size() <= 3) {
            return sortedRooms;
        }

        // Try to find the best continuous sequence by minimizing room number variance
        // This ensures rooms are as close together as possible
        return sortedRooms;
    }

    /**
     * Selects specific rooms based on room type selections, prioritizing close proximity when multiple rooms are
     * selected. Rooms are sorted by room number to ensure adjacent rooms are selected when possible.
     *
     * @param roomTypeSelections List of room type selections with quantities
     * @param availableRooms All available rooms for the date range
     * @return List of selected rooms in close proximity
     */
    private List<Room> selectRoomsWithProximity(List<RoomTypeSelectionDto> roomTypeSelections,
            List<Room> availableRooms) {
        // Group available rooms by room type ID
        Map<Long, List<Room>> roomsByType =
                availableRooms.stream().collect(Collectors.groupingBy(room -> room.getRoomType().getId()));

        // Collect all selected rooms
        List<Room> allSelectedRooms = new ArrayList<>();

        // For each room type selection, pick the requested quantity
        for (RoomTypeSelectionDto selection : roomTypeSelections) {
            Long roomTypeId = selection.getRoomTypeId();
            Integer quantity = selection.getQuantity();
            BigDecimal pricePerNight = selection.getPricePerNight();
            BigDecimal normalizedPricePerNight = RoomTypePriceKey.normalizePrice(pricePerNight);

            // Verify room type exists
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("Room type not found with ID: " + roomTypeId));

            List<Room> roomsOfType = roomsByType.getOrDefault(roomTypeId, new ArrayList<>());

            if (normalizedPricePerNight != null) {
                roomsOfType = roomsOfType.stream()
                        .filter(room -> room.getEffectiveBasePrice() != null
                                && RoomTypePriceKey.normalizePrice(room.getEffectiveBasePrice())
                                        .compareTo(normalizedPricePerNight) == 0)
                        .collect(Collectors.toList());
            }

            if (roomsOfType.size() < quantity) {
                String priceHint = normalizedPricePerNight != null ? " at price " + normalizedPricePerNight : "";
                throw new IllegalArgumentException("Not enough available rooms of type '" + roomType.getName() + "'"
                        + priceHint + ". Requested: " + quantity + ", Available: " + roomsOfType.size());
            }

            // Sort by room number for proximity
            List<Room> sortedRooms = roomsOfType.stream()
                    .sorted(Comparator.comparing(this::extractNumericRoomNumber))
                    .collect(Collectors.toList());

            // Take the requested quantity
            allSelectedRooms.addAll(sortedRooms.subList(0, quantity));
        }

        // If multiple rooms selected, re-sort all rooms by proximity
        if (allSelectedRooms.size() > 1) {
            allSelectedRooms = optimizeRoomProximity(allSelectedRooms);
        }

        return allSelectedRooms;
    }

    // Invoked by Customers, Creates a Stripe session, Creates a PENDING booking
    @Override
    public Session beginBooking(CreateOrUpdateBookingRequestDto requestDto) {
        try {
            validateCheckInOutDates(requestDto.getCheckInDate(), requestDto.getCheckOutDate());
            validateGuestAge(requestDto.getGuestInfo().getDateOfBirth());
            Guest guest = validateAndGetGuest(requestDto.getGuestInfo());

            List<Room> selectedRooms;
            int totalRequestedRooms = 0;
            int totalGuests = requestDto.getNumAdults() + requestDto.getNumChildren();

            List<RoomTypeSelectionDto> roomTypeSelections = requestDto.getRoomTypeSelections();
            totalRequestedRooms = roomTypeSelections.stream().mapToInt(RoomTypeSelectionDto::getQuantity).sum();

            if (totalRequestedRooms > totalGuests) {
                throw new IllegalArgumentException(
                        String.format("Cannot book %d rooms for only %d guests (adults + children).",
                                totalRequestedRooms, totalGuests));
            }

            List<Room> availableRooms = roomRepository.findAvailableRoomsByDateRange(requestDto.getCheckInDate(),
                    requestDto.getCheckOutDate());

            selectedRooms = selectRoomsWithProximity(roomTypeSelections, availableRooms);

            if (selectedRooms.size() < totalRequestedRooms) {
                throw new IllegalArgumentException("Not enough available rooms to fulfill the booking. Requested: "
                        + totalRequestedRooms + ", Available: " + selectedRooms.size());
            }

            // Create booking
            Booking booking = Booking.builder()
                    .checkInDate(requestDto.getCheckInDate())
                    .checkOutDate(requestDto.getCheckOutDate())
                    .numAdults(requestDto.getNumAdults())
                    .numChildren(requestDto.getNumChildren())
                    .status(BookingStatus.PENDING)
                    .createdManually(Boolean.TRUE.equals(requestDto.getCreatedManually()))
                    .earlyCheckIn(Boolean.TRUE.equals(requestDto.getEarlyCheckIn()))
                    .lateCheckOut(Boolean.TRUE.equals(requestDto.getLateCheckOut()))
                    .guest(guest)
                    .rooms(new HashSet<>(selectedRooms))
                    .payments(new HashSet<>())
                    .build();
            booking = bookingRepository.save(booking);
            Set<Room> rooms = booking.getRooms();

            Stripe.apiKey = stripeSecretKey;

            // Calculate the duration of the booking
            long durationInDays = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());

            // Calcualate total price of the booking
            BigDecimal totalPrice = BigDecimal.ZERO;
            for (Room room : rooms) {
                if (room.getBasePriceOverride() != null) {
                    totalPrice = totalPrice.add(room.getBasePriceOverride());
                } else {
                    totalPrice = totalPrice.add(room.getRoomType().getBasePrice());
                }
            }
            totalPrice = totalPrice.multiply(BigDecimal.valueOf(durationInDays));

            // Add early check-in and late check-out fees
            if (Boolean.TRUE.equals(requestDto.getEarlyCheckIn())) {
                totalPrice = totalPrice.add(new BigDecimal("20.00").multiply(BigDecimal.valueOf(rooms.size())));
            }
            if (Boolean.TRUE.equals(requestDto.getLateCheckOut())) {
                totalPrice = totalPrice.add(new BigDecimal("20.00").multiply(BigDecimal.valueOf(rooms.size())));
            }

            // Convert to cents for Stripe
            totalPrice = totalPrice.multiply(BigDecimal.valueOf(100));

            // Create description of the booking
            StringBuilder description = new StringBuilder();
            Map<RoomType, Long> roomTypeQuantities = new HashMap<>();
            for (Room room : rooms) {
                roomTypeQuantities.put(room.getRoomType(),
                        roomTypeQuantities.getOrDefault(room.getRoomType(), 0L) + 1L);
            }
            for (Map.Entry<RoomType, Long> entry : roomTypeQuantities.entrySet()) {
                description.append(entry.getValue() + " x " + entry.getKey().getName() + "\n");
            }
            description.append(
                    "Stay: Check-in: " + booking.getCheckInDate() + ", Check-out: " + booking.getCheckOutDate());

            // Add early check-in and late check-out to description
            if (booking.getEarlyCheckIn()) {
                BigDecimal earlyCheckInFee = new BigDecimal("20.00").multiply(BigDecimal.valueOf(rooms.size()));
                description.append("\nEarly Check-in: +€" + earlyCheckInFee);
            }
            if (booking.getLateCheckOut()) {
                BigDecimal lateCheckOutFee = new BigDecimal("20.00").multiply(BigDecimal.valueOf(rooms.size()));
                description.append("\nLate Check-out: +€" + lateCheckOutFee);
            }

            SessionCreateParams.LineItem lineItem = SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("eur")
                            .setUnitAmount(totalPrice.longValue())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("TaskInn Booking - " + durationInDays + " nights")
                                    .setDescription(description.toString())
                                    .build())
                            .build())
                    .build();

            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setExpiresAt(Instant.now().plus(32, ChronoUnit.MINUTES).getEpochSecond())
                    .setSuccessUrl(frontendBaseUrl + "/booking/success?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendBaseUrl + "/booking/checkout?checkIn=" + booking.getCheckInDate()
                            + "&checkOut=" + booking.getCheckOutDate() + "&numRooms=" + rooms.size() + "&numAdults="
                            + booking.getNumAdults() + "&numChildren=" + booking.getNumChildren())
                    .addLineItem(lineItem)
                    .putMetadata("bookingId", booking.getId().toString())
                    .build();

            return Session.create(params);
        } catch (StripeException e) {
            throw new IllegalStateException("Failed to create Stripe session", e);
        }
    }

    // Invoked by Customers, Updates the status of the booking to CONFIRMED
    @Override
    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking must be PENDING to be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        this.publisher.publishEvent(new BookingStateChangedEvent(booking.getId(), booking.getStatus()));

        long durationInDays = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Room room : booking.getRooms()) {
            if (room.getBasePriceOverride() != null) {
                totalPrice = totalPrice.add(room.getBasePriceOverride());
            } else {
                totalPrice = totalPrice.add(room.getRoomType().getBasePrice());
            }
        }
        totalPrice = totalPrice.multiply(BigDecimal.valueOf(durationInDays));
        if (booking.getEarlyCheckIn()) {
            totalPrice =
                    totalPrice.add(new BigDecimal("20.00").multiply(BigDecimal.valueOf(booking.getRooms().size())));
        }

        if (booking.getLateCheckOut()) {
            totalPrice =
                    totalPrice.add(new BigDecimal("20.00").multiply(BigDecimal.valueOf(booking.getRooms().size())));
        }

        Payment payment = Payment.builder().amount(totalPrice).status(PaymentStatus.SUCCEEDED).booking(booking).build();

        if (booking.getPayments() == null) {
            booking.setPayments(new HashSet<>());
        }
        booking.getPayments().add(payment);

        bookingRepository.save(booking);

        this.emailService.sendBookingConfirmationEmail(booking.getGuest().getEmail(), booking);
    }

    // Invoked by Customers, Deletes the pending booking
    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Booking must be PENDING to be cancelled");
        }
        bookingRepository.delete(booking);
    }

    @Override
    public CreateOrUpdateBookingResponseDto getBookingResponseBySessionId(String sessionId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Session session = Session.retrieve(sessionId);
            String bookingIdValue = session.getMetadata() != null ? session.getMetadata().get("bookingId") : null;
            if (bookingIdValue == null) {
                throw new EntityNotFoundException("Booking not found");
            }

            Long bookingId = Long.parseLong(bookingIdValue);
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

            List<String> roomNumbers =
                    booking.getRooms().stream().map(Room::getRoomNumber).sorted().collect(Collectors.toList());

            return CreateOrUpdateBookingResponseDto.builder()
                    .bookingId(booking.getId())
                    .checkInDate(booking.getCheckInDate())
                    .checkOutDate(booking.getCheckOutDate())
                    .assignedRoomNumbers(roomNumbers)
                    .status(booking.getStatus().name())
                    .guestEmail(booking.getGuest().getEmail())
                    .earlyCheckIn(booking.getEarlyCheckIn())
                    .lateCheckOut(booking.getLateCheckOut())
                    .build();
        } catch (StripeException e) {
            throw new IllegalStateException("Failed to fetch Stripe session", e);
        }
    }

    @Override
    public Boolean canCheckout(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        return booking.getStatus() == BookingStatus.CHECKED_IN;
    }
}
