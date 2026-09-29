package tuwien.taskinn.exceptions;

/*
 * Custom exception for room conflicts, such as attempting to disable a room that is currently booked.
 */
public class RoomConflictException extends RuntimeException {

    public RoomConflictException(String message) {
        super(message);
    }
}
