package tuwien.taskinn.services.validator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.entity.Booking;
import tuwien.taskinn.entity.Room;
import tuwien.taskinn.enums.BookingStatus;
import tuwien.taskinn.exceptions.BusinessValidationException;
import tuwien.taskinn.repo.BookingRepository;
import tuwien.taskinn.repo.RoomRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CheckInValidator {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9. ()-]{7,25}$");

    /**
     * @implNote Booking exists by id
     * @implNote Booking check-in date is today
     * @implNote Booking status is CONFIRMED
     */
    public void validateBookingStatusForCheckin(Long bookingId) {
        // booking exists by id
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        // booking check-in date is today
        if (!booking.getCheckInDate().equals(LocalDate.now())) {
            throw new BusinessValidationException("booking",
                    "Check-in is only allowed on the booking's check-in date (" + booking.getCheckInDate() + ")");
        }

        // booking status is CONFIRMED
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessValidationException("booking",
                    "Booking must be in CONFIRMED status to check in. Current status: " + booking.getStatus());
        }
    }

    /**
     * @implNote All checks from validateBookingStatusForCheckin
     * @implNote Total number of guests in dto <= number of occupants in booking
     * @implNote At least one guest has a non-blank email
     * @implNote At least one guest has a non-blank phone
     * @implNote No duplicate emails in dto
     * @implNote Room ids of guests belong to the booking
     * @implNote Each room exists by id
     * @implNote Each room has at least one guest
     * @implNote Each room has capacity >= number of guests
     * @implNote Each room has status ACTIVE
     * @implNote Room ids of guests belong to the booking
     */
    public void validateCheckin(Long bookingId, List<CheckInGuestDto> guestDtos, Map<String, MultipartFile> files) {
        validateBookingStatusForCheckin(bookingId);

        Booking booking = bookingRepository.findById(bookingId).get();

        // total number of guests in dto <= number of occupants in booking
        int bookingTotalGuests =
                booking.getNumAdults() + (booking.getNumChildren() != null ? booking.getNumChildren() : 0);
        int dtoTotalGuests = guestDtos.size();
        if (dtoTotalGuests > bookingTotalGuests) {
            throw new BusinessValidationException("guests", "Number of guests (" + dtoTotalGuests
                    + ") exceeds the booking capacity (" + bookingTotalGuests + ")");
        }

        boolean hasEmail = false;
        boolean hasPhone = false;

        Map<String, Integer> emailCountMap = new HashMap<>();
        Map<Long, Integer> guestCountPerRoom = new HashMap<>();

        for (int i = 0; i < guestDtos.size(); i++) {
            CheckInGuestDto guestDto = guestDtos.get(i);
            Long roomId = guestDto.getRoomId();
            guestCountPerRoom.put(roomId, guestCountPerRoom.getOrDefault(roomId, 0) + 1);

            String email = guestDto.getEmail();

            if (!hasEmail && email != null && !email.isBlank()) {
                hasEmail = true;
            }
            if (!hasPhone && guestDto.getPhoneNumber() != null && !guestDto.getPhoneNumber().isBlank()) {
                hasPhone = true;
            }

            if (email != null && !email.isBlank()) {
                emailCountMap.put(email.toLowerCase(), emailCountMap.getOrDefault(email.toLowerCase(), 0) + 1);
            }

            validateGuest(guestDto, i);
        }

        // at least one guest has a non-blank email
        if (!hasEmail) {
            throw new BusinessValidationException("email",
                    "At least one guest must have an email address for contact purposes");
        }
        // at least one guest has a non-blank phone
        if (!hasPhone) {
            throw new BusinessValidationException("phoneNumber",
                    "At least one guest must have a phone number for contact purposes");
        }

        // no duplicate emails in dto
        for (Map.Entry<String, Integer> entry : emailCountMap.entrySet()) {
            if (entry.getValue() > 1) {
                throw new BusinessValidationException("email",
                        "Duplicate email address found: '" + entry.getKey() + "'. Each guest must have a unique email");
            }
        }

        for (Map.Entry<Long, Integer> entry : guestCountPerRoom.entrySet()) {
            Long roomId = entry.getKey();
            int count = entry.getValue();

            // each room exists by id
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new EntityNotFoundException("Room with id " + roomId + " not found"));

            // each room has at least one guest
            if (entry.getValue() < 1) {
                throw new BusinessValidationException("room",
                        "Room " + room.getRoomNumber() + " must have at least one guest assigned");
            }

            // each room has status ACTIVE
            if (!room.isActive()) {
                throw new BusinessValidationException("room", "Room " + room.getRoomNumber()
                        + " is currently inactive. Please contact staff to update the booking");
            }

            // each room has capacity >= number of guests
            int capacity = room.getRoomType() != null ? room.getRoomType().getCapacity() : 0;
            if (count > capacity) {
                throw new BusinessValidationException("room", "Room " + room.getRoomNumber() + " can only accommodate "
                        + capacity + " guest(s), but " + count + " were assigned");
            }
        }

        // room ids of guests belong to the booking
        for (CheckInGuestDto guestDto : guestDtos) {
            Long roomId = guestDto.getRoomId();
            boolean roomInBooking = booking.getRooms().stream().anyMatch(br -> br.getId().equals(roomId));
            if (!roomInBooking) {
                throw new BusinessValidationException("room", "Room ID " + roomId + " is not part of this booking");
            }
        }

    }

    public void validateImage(String base64Image, byte[] decodedImage, int guestIndex) {
        String guestLabel = "Guest " + (guestIndex + 1);
        if (base64Image == null || base64Image.isEmpty()) {
            throw new BusinessValidationException("idImage", guestLabel + ": ID image is required");
        }
        if (decodedImage.length > 10 * 1024 * 1024) {
            throw new BusinessValidationException("idImage", guestLabel + ": ID image size exceeds the 10MB limit");
        }
    }

    public void validateGuest(CheckInGuestDto guest, int guestIndex) {
        String guestLabel = "Guest " + (guestIndex + 1);
        if (!StringUtils.hasText(guest.getFirstName())) {
            throw new BusinessValidationException("firstName", guestLabel + ": First name is required");
        }
        if (!StringUtils.hasText(guest.getLastName())) {
            throw new BusinessValidationException("lastName", guestLabel + ": Last name is required");
        }
        if (guest.getDateOfBirth() == null) {
            throw new BusinessValidationException("dateOfBirth", guestLabel + ": Date of birth is required");
        }
        if (guest.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new BusinessValidationException("dateOfBirth",
                    guestLabel + ": Date of birth cannot be in the future");
        }
        if (guest.getEmail() != null && !guest.getEmail().isBlank()) {
            if (!isValidEmail(guest.getEmail())) {
                throw new BusinessValidationException("email", guestLabel + ": Email address format is invalid");
            }
        }
        if (guest.getPhoneNumber() != null && !guest.getPhoneNumber().isBlank()) {
            if (!isValidPhone(guest.getPhoneNumber())) {
                throw new BusinessValidationException("phoneNumber", guestLabel + ": Phone number format is invalid");
            }
        }
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }
}
