package tuwien.taskinn.services.scheduling.instance;

import lombok.Getter;

import java.util.List;
import java.util.stream.Stream;

/**
 * Representation of a concrete task scheduling problem instance. Describes a number of room tasks that need to be
 * distributed to a fixed number of staff members for a given day.
 */
public class TaskSchedulingInstance implements ProblemInstance {

    /**
     * Represents all the information that is needed for a single room task.
     *
     * @param identifier unique identifier for the room (to be used by the interface consumer)
     * @param posX x position of the room's midpoint
     * @param posY y position of the room's midpoint
     * @param taskDuration time that the task is expected to take (in minutes)
     * @param maxEndTime maximum end time of the task (in minutes where 0 is the start of the day's tasks, negative for
     *        unconstrained)
     * @param minStartTime minimum start time of the task (in minutes where 0 is the start of the day's tasks, negative
     *        for unconstrained)
     */
    public record RoomTask(long identifier, double posX, double posY, int floor, int taskDuration, int maxEndTime,
            int minStartTime) {
    }

    @Getter
    private final int staffMemberCount;
    private final List<RoomTask> rooms;
    private final double[][] distances;

    /**
     * Constructs a TaskSchedulingInstance with then given number of staff and room information.
     *
     * @param pStaffMemberCount the given number of staff members
     * @param pRooms the given room data
     */
    public TaskSchedulingInstance(int pStaffMemberCount, Stream<RoomTask> pRooms) {
        staffMemberCount = pStaffMemberCount;
        rooms = pRooms.toList();

        distances = new double[rooms.size()][rooms.size()];
        for (int i = 0; i < rooms.size(); i++) {
            for (int j = 0; j < rooms.size(); j++) {
                if (i == j) {
                    continue;
                }
                final var roomI = rooms.get(i);
                final var roomJ = rooms.get(j);
                final double horizontalDistance = Math
                        .sqrt(Math.pow(roomI.posX() - roomJ.posX(), 2.0) + Math.pow(roomI.posY() - roomJ.posY(), 2.0))
                        * 0.036;
                final double verticalDistance =
                        (roomI.floor == roomJ.floor) ? 0.0 : 10.0 + (Math.abs(roomI.floor - roomJ.floor) * 2.0);

                distances[i][j] = (horizontalDistance + verticalDistance);
            }
        }
    }

    /**
     * Returns the number of rooms in this instance.
     *
     * @return the number of rooms in this instance
     */
    public int getRoomCount() {
        return rooms.size();
    }

    /**
     * Returns the room data for a room at the given index.
     *
     * @param index the given index
     * @return the room data for the room at the given index
     */
    public RoomTask getRoom(int index) {
        return rooms.get(index);
    }

    /**
     * Returns the distance between two rooms with the given indices.
     *
     * @param firstIndex the given index of the first room
     * @param secondIndex the given index of the second room
     * @return the distance between the two rooms with the given indices
     */
    public double getRoomDistance(int firstIndex, int secondIndex) {
        return distances[firstIndex][secondIndex];
    }

    @Override
    public boolean isInfeasible() {
        return false;
    }

    @Override
    public long instanceSeed() {
        return 0xBEEFL;
    }

}
