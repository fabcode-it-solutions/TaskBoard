package tuwien.taskinn.mapper;

import org.springframework.stereotype.Component;
import tuwien.taskinn.dto.ShiftScheduleTemplateDto;
import tuwien.taskinn.entity.ShiftScheduleTemplate;

@Component
public class SchedulingMapper {

    public ShiftScheduleTemplateDto toDto(ShiftScheduleTemplate template) {
        if (template == null) {
            return null;
        }

        return ShiftScheduleTemplateDto.builder()
                .id(template.getId())
                .name(template.getName())
                .content(template.getContent())
                .build();
    }

}
