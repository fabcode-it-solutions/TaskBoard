package tuwien.taskinn.services.scheduling.solver;

import tuwien.taskinn.services.scheduling.instance.TaskSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.TaskSchedulingSolution;

import java.util.Random;

/**
 * A concrete notion of a neighborhood for solutions of a given task scheduling problem. A neighbor of a given solution
 * is defined as equivalent to removing a room from a staff member's task list and adding it to another one's.
 */
public class TaskScheduleReassignmentNeighborhood
        implements SolutionNeighborhood<TaskSchedulingSolution, TaskSchedulingInstance> {

    @Override
    public TaskSchedulingSolution randomNeighbor(Random random, TaskSchedulingInstance instance,
            TaskSchedulingSolution solution) {
        final var assignments = solution.copyAssignments();
        if (instance.getStaffMemberCount() == 1 || random.nextDouble() < 0.5) {
            // Reassign within a single staff member
            final int staffIndex = random.nextInt(instance.getStaffMemberCount());
            final var staffAssignments = assignments.get(staffIndex);
            final int roomCount = solution.getAssignmentCount(staffIndex);
            if (roomCount >= 2) {
                final int roomIndex1 = random.nextInt(roomCount);
                final int roomIndex2 = (roomIndex1 + random.nextInt(roomCount - 1) + 1) % roomCount;
                final int tmp = staffAssignments.get(roomIndex1);
                staffAssignments.set(roomIndex1, staffAssignments.get(roomIndex2));
                staffAssignments.set(roomIndex2, tmp);
            }
        } else if (instance.getStaffMemberCount() > 1) {
            // Reassign from one staff member to another
            final int staffCount = instance.getStaffMemberCount();
            final int staffIndex1 = random.nextInt(staffCount);
            final int staffIndex2 = (staffIndex1 + random.nextInt(staffCount - 1) + 1) % staffCount;
            final int roomCount1 = solution.getAssignmentCount(staffIndex1);
            final int roomCount2 = solution.getAssignmentCount(staffIndex2);
            if (roomCount1 > 0) {
                final int roomIndex1 = random.nextInt(roomCount1);
                final int roomIndex2 = random.nextInt(roomCount2 + 1);
                final int reassignedRoom = assignments.get(staffIndex1).remove(roomIndex1);
                assignments.get(staffIndex2).add(roomIndex2, reassignedRoom);
            }
        }
        return new TaskSchedulingSolution(assignments);
    }

}
