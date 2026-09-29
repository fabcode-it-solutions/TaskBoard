package tuwien.taskinn.events;

import tuwien.taskinn.enums.BookingStatus;

public record BookingStateChangedEvent(Long bookingId, BookingStatus newStatus) {
}
