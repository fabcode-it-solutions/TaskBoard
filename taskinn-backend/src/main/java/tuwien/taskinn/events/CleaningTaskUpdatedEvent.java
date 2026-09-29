package tuwien.taskinn.events;

import tuwien.taskinn.enums.RoomStatus;

public record CleaningTaskUpdatedEvent(Long roomId, RoomStatus newRoomStatus) {
}
