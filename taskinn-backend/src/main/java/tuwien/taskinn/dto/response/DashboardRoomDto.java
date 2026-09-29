package tuwien.taskinn.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tuwien.taskinn.dto.PointDto;
import tuwien.taskinn.enums.RoomStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRoomDto {

    private Long roomId;

    private String roomNumber;

    private RoomStatus status;

    private String guestName;

    private Integer floorNumber;

    private List<PointDto> svgPoints;

    private boolean hasOpenMaintenanceTicket;
}
