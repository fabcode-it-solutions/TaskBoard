package tuwien.taskinn.services;

import tuwien.taskinn.dto.response.DashboardRoomDto;

import java.util.List;

public interface RoomDashboardService {

    /**
     * Gets the current states of all rooms for the dashboard view, including their status, assigned guest names, and
     * maintenance ticket status. This method is to be called when initializing the dashboard to provide an overview of
     * room occupancy and availability.
     *
     * @return A list of {@link DashboardRoomDto} representing the current state of each room.
     */
    List<DashboardRoomDto> getCurrentRoomStates();

    /**
     * Gets the updated state of a specific room for real-time dashboard updates. This method is to be called when a
     * room's status changes (e.g., check-in, check-out, maintenance ticket update) to provide the latest information
     * for that room.
     *
     * @param roomId the ID of the room to retrieve the updated state for
     * @return A {@link DashboardRoomDto} representing the updated state of the specified room.
     */
    DashboardRoomDto getUpdatedStateForRoom(Long roomId);
}
