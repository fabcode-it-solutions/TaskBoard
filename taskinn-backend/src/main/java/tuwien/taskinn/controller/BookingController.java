package tuwien.taskinn.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.annotation.RequireRoles;
import tuwien.taskinn.dto.request.CheckInGuestDto;
import tuwien.taskinn.dto.request.CreateOrUpdateBookingRequestDto;
import tuwien.taskinn.dto.request.RoomOptionsRequestDto;
import tuwien.taskinn.dto.response.*;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.BookingService;

@Validated
@RestController
@RequestMapping("/api/v1/booking")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @Value("${stripe.webhook-secret}")
    private String stripeWebhookSecret;

    @PostMapping("/room-options")
    public ResponseEntity<List<RoomTypeOptionDto>> getRoomOptions(
            @RequestBody @Valid RoomOptionsRequestDto requestDto) {
        return ResponseEntity.ok(bookingService.getRoomTypeOptions(requestDto));
    }

    @PostMapping("/create-booking")
    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    public ResponseEntity<CreateOrUpdateBookingResponseDto> createBooking(
            @RequestBody @Valid CreateOrUpdateBookingRequestDto requestDto) {
        CreateOrUpdateBookingResponseDto response = bookingService.createBooking(requestDto);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    public ResponseEntity<Void> updateBooking(@RequestBody @Valid CreateOrUpdateBookingRequestDto requestDto,
            @RequestParam Long bookingId) {
        bookingService.updateBooking(bookingId, requestDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/begin-booking")
    public ResponseEntity<Map<String, String>> beginBooking(@RequestBody CreateOrUpdateBookingRequestDto requestDto) {
        Session session = bookingService.beginBooking(requestDto);
        return ResponseEntity.ok(Map.of("url", session.getUrl()));
    }

    @PostMapping("/stripe-webhook")
    public ResponseEntity<Void> confirmBooking(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        // 1. Verify the webhook signature
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            System.out.println("Invalid Signature: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // 2. Handle the event
        String eventType = event.getType();
        switch (eventType) {
            case "checkout.session.completed":
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                Long bookingId = Long.parseLong(session.getMetadata().get("bookingId"));
                bookingService.confirmBooking(bookingId);
                break;
            case "checkout.session.expired":
            case "checkout.session.async_payment_failed":
                Session session2 = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                Long bookingId2 = Long.parseLong(session2.getMetadata().get("bookingId"));
                bookingService.cancelBooking(bookingId2);
                break;
            case "payment_intent.succeeded":
            case "payment_intent.created":
            case "charge.succeeded":
            case "charge.updated":
                // These events we know about, but we don't need to handle them
                break;

            default:
                // These events are not handled
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/checkout-session")
    public ResponseEntity<CreateOrUpdateBookingResponseDto> getBookingFromSession(
            @RequestParam("session_id") String sessionId) {
        CreateOrUpdateBookingResponseDto response = bookingService.getBookingResponseBySessionId(sessionId);
        return ResponseEntity.ok(response);
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @GetMapping("/{bookingId}/checkin/rooms")
    public ResponseEntity<List<RoomForCheckinResponseDto>> getRoomsForCheckin(@PathVariable Long bookingId) {
        List<RoomForCheckinResponseDto> rooms = bookingService.getRoomsForCheckinByBookingId(bookingId);
        return ResponseEntity.ok(rooms);
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @PostMapping(value = "/{bookingId}/checkin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> checkInBooking(@PathVariable Long bookingId,
            @RequestPart("guests") List<CheckInGuestDto> guestDtos, @RequestParam Map<String, MultipartFile> files) {
        bookingService.checkInBooking(bookingId, guestDtos, files);
        return ResponseEntity.ok().build();
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @PutMapping("/{bookingId}/checkout")
    public ResponseEntity<Void> checkOutBooking(@PathVariable Long bookingId) {
        bookingService.checkOutBooking(bookingId);
        return ResponseEntity.ok().build();
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @GetMapping("/{bookingId}")
    public ResponseEntity<DetailedBookingResponseDto> getBookingDetails(@PathVariable Long bookingId) {
        DetailedBookingResponseDto bookingDetails = bookingService.getBookingDetails(bookingId);
        return ResponseEntity.status(HttpStatus.OK).body(bookingDetails);
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @GetMapping()
    public ResponseEntity<List<BookingSummaryResponseDto>> getAllBookings() {
        List<BookingSummaryResponseDto> bookings = bookingService.findAllBookings();
        return ResponseEntity.status(HttpStatus.OK).body(bookings);
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @GetMapping("/checkins/today/count")
    public ResponseEntity<Long> getTodaysCheckInCount() {
        Long count = bookingService.getTodaysCheckInCount();
        return ResponseEntity.ok(count);
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @GetMapping("/checkouts/today/count")
    public ResponseEntity<Long> getTodaysCheckOutCount() {
        Long count = bookingService.getTodaysCheckOutCount();
        return ResponseEntity.ok(count);
    }

    @RequireRoles({@RequireRole(Roles.EMPLOYEE), @RequireRole(Roles.ADMIN)})
    @GetMapping("/{bookingId}/can-checkout")
    public ResponseEntity<Boolean> canCheckout(@PathVariable Long bookingId) {
        Boolean canCheckout = bookingService.canCheckout(bookingId);
        return ResponseEntity.ok(canCheckout);
    }

    @RequireRoles({@RequireRole(Roles.ADMIN), @RequireRole(Roles.EMPLOYEE)})
    @GetMapping("/constrained-rooms/{year}/{month}/{day}")
    public ResponseEntity<SpecialBookingConstraintsDto> getSpecialBookingConstraints(
            @PathVariable @NotNull @Min(2024) Integer year, @PathVariable @NotNull @Min(1) @Max(12) Integer month,
            @PathVariable @NotNull @Min(1) @Max(31) Integer day) {
        final SpecialBookingConstraintsDto result = bookingService.getSpecialBookingConstraints(year, month, day);
        return ResponseEntity.ok(result);
    }
}
