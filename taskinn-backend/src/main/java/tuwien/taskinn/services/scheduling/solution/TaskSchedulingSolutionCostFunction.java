package tuwien.taskinn.services.scheduling.solution;

import tuwien.taskinn.services.scheduling.instance.TaskSchedulingInstance;

/**
 * A concrete cost function for task scheduling problems.
 */
public class TaskSchedulingSolutionCostFunction
        implements CostFunction<Double, TaskSchedulingInstance, TaskSchedulingSolution> {

    /**
     * Represents all data that goes into the solution cost. This is to have an effectively final object that can be
     * manipulated from within a lambda expression.
     */
    private static class CostData {
        public double maxEndOverrun = 0.0;
        public int maxEndOverrunCount = 0;
        public double lastTaskEnd = 0.0;
        public double currentTaskEnd = 0.0;
        public double workTime = 0.0;
    }

    @Override
    public Double cost(TaskSchedulingInstance instance, TaskSchedulingSolution solution) {
        final CostData costData = new CostData();

        double minWorkTime = Double.MAX_VALUE;
        double maxWorkTime = Double.MIN_VALUE;
        for (int staffIndex = 0; staffIndex < instance.getStaffMemberCount(); staffIndex++) {
            costData.currentTaskEnd = 0.0;
            costData.workTime = 0.0;
            applySolutionRow(instance, solution, staffIndex, (room, currentTime) -> {
                costData.currentTaskEnd = Math.max(costData.currentTaskEnd, currentTime + room.taskDuration());
                if (room.maxEndTime() >= 0 && (currentTime + room.taskDuration()) > room.maxEndTime()) {
                    costData.maxEndOverrunCount++;
                    costData.workTime += room.taskDuration();
                    costData.maxEndOverrun += ((currentTime + room.taskDuration()) - room.maxEndTime());
                }
            });
            costData.lastTaskEnd = Math.max(costData.lastTaskEnd, costData.currentTaskEnd);
            minWorkTime = Math.min(minWorkTime, costData.workTime);
            maxWorkTime = Math.max(maxWorkTime, costData.workTime);
        }
        if (minWorkTime == Double.MAX_VALUE || maxWorkTime == Double.MIN_VALUE) {
            minWorkTime = 0.0;
            maxWorkTime = 0.0;
        }
        final double timeDiscrepancy = (maxWorkTime - minWorkTime) / 2.0;

        return timeDiscrepancy + costData.lastTaskEnd + (100.0 * costData.maxEndOverrun)
                + (1_000.0 * (double) costData.maxEndOverrunCount);
    }

    /**
     * Represents a function that can be applied to the task list for a single staff member.
     */
    public interface SolutionRowFunction {
        void apply(TaskSchedulingInstance.RoomTask room, double currentTime);
    }

    /**
     * Applies a given function to the task list of a single given staff member in a given solution to a given problem
     * instance. This is to centralize the logic for reconstructing task lists from a solution and make it available to
     * external consumers as well.
     *
     * @param instance the given problem instance
     * @param solution the given solution
     * @param staffIndex the index of the given staff member
     * @param function the function to apply to the given staff member's task list
     */
    public static void applySolutionRow(TaskSchedulingInstance instance, TaskSchedulingSolution solution,
            int staffIndex, SolutionRowFunction function) {
        double currentTime = 0.0;
        double continuousWorkTime = 0.0;
        int lastRoomIndex = -1;
        for (int taskIndex = 0; taskIndex < solution.getAssignmentCount(staffIndex); taskIndex++) {
            final int roomIndex = solution.getAssignments(staffIndex, taskIndex);
            final var room = instance.getRoom(roomIndex);
            final double transferTime = (lastRoomIndex >= 0 ? instance.getRoomDistance(lastRoomIndex, roomIndex) : 0.0);
            lastRoomIndex = roomIndex;
            if (room.minStartTime() >= 0 && currentTime < room.minStartTime()) {
                currentTime = room.minStartTime();
                continuousWorkTime = 0.0;
            } else if ((continuousWorkTime + (room.taskDuration() + transferTime)) > 240.0) {
                // Take a break
                currentTime += 30.0;
                continuousWorkTime = 0.0;
            } else {
                currentTime += transferTime;
                continuousWorkTime += transferTime;
            }
            function.apply(room, currentTime);
            currentTime += room.taskDuration();
            continuousWorkTime += room.taskDuration();
        }
    }

    @Override
    public int compareCost(Double left, Double right) {
        return left.compareTo(right);
    }

    @Override
    public double costDistance(Double score) {
        return score;
    }

    @Override
    public double costDistance(Double left, Double right) {
        return left - right;
    }

}
