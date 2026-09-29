package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.PtoDto;
import tuwien.taskinn.entity.PTO;

/**
 * Mapper for converting between PTO entity and PtoDto.
 */
@Component
public class PtoMapper {

    /**
     * Converts a PTO entity to a PtoDto.
     *
     * @param pto the PTO entity to convert
     * @return the converted PtoDto
     */
    public PtoDto toDto(PTO pto) {
        if (pto == null) {
            return null;
        }

        return PtoDto.builder().id(pto.getId()).startDate(pto.getStartDate()).endDate(pto.getEndDate()).build();
    }
}
