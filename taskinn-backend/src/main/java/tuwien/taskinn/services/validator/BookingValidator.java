package tuwien.taskinn.services.validator;

import org.springframework.stereotype.Component;

import tuwien.taskinn.dto.request.RoomOptionsRequestDto;
import tuwien.taskinn.exceptions.BusinessValidationException;

@Component
public class BookingValidator {
    public void validateRoomOptionsRequestDto(RoomOptionsRequestDto roomOptionsRequestDto) {
        if (roomOptionsRequestDto.getCheckInDate().isAfter(roomOptionsRequestDto.getCheckOutDate())) {
            throw new BusinessValidationException("checkOutDate", "Check-out date must be after check-in date");
        }
    }
}
