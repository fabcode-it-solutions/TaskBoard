package tuwien.taskinn.services.validator;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import tuwien.taskinn.dto.request.CreatePtoRequest;
import tuwien.taskinn.entity.PTO;
import tuwien.taskinn.entity.Shift;
import tuwien.taskinn.exceptions.BusinessValidationException;

@Component
public class EmployeeValidator {
    public void validateCreatePtoRequestDto(CreatePtoRequest createPtoRequestDto, List<PTO> existingPtos,
            List<Shift> existingShifts) {
        if (createPtoRequestDto.getStartDate().isAfter(createPtoRequestDto.getEndDate())) {
            throw new BusinessValidationException("startDate", "Start date must be before end date");
        }

        for (PTO existingPto : existingPtos) {
            if (!createPtoRequestDto.getEndDate().isBefore(existingPto.getStartDate())
                    && !createPtoRequestDto.getStartDate().isAfter(existingPto.getEndDate())) {
                throw new BusinessValidationException("endDate", "The selected period overlaps with an existing PTO");
            }
        }

        // Check if PTO overlaps with any scheduled shifts
        for (Shift shift : existingShifts) {
            LocalDate shiftDate = shift.getDate().toLocalDate();
            if (!shiftDate.isBefore(createPtoRequestDto.getStartDate())
                    && !shiftDate.isAfter(createPtoRequestDto.getEndDate())) {
                throw new BusinessValidationException("startDate",
                        "Cannot request PTO on days you are scheduled to work");
            }
        }
    }
}
