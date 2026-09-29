package tuwien.taskinn.services.scheduling.solver;

import java.util.ArrayList;
import java.util.Random;
import tuwien.taskinn.services.scheduling.instance.ShiftSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.ShiftSchedulingSolution;
import tuwien.taskinn.services.scheduling.utility.BitMatrix;

/**
 * A concrete notion of a neighborhood for solutions of a given shift scheduling problem. A neighbor of a given solution
 * is defined as having exactly one pair of staff members swap their assignments on exactly one day.
 */
public class ShiftScheduleDailyReassignmentNeighborhood
        implements SolutionNeighborhood<ShiftSchedulingSolution, ShiftSchedulingInstance> {

    @Override
    public ShiftSchedulingSolution randomNeighbor(Random random, ShiftSchedulingInstance instance,
            ShiftSchedulingSolution solution) {
        final int chosenDay = random.nextInt(instance.getDayCount());
        // To hold all staff members assigned/not assigned on the chosen day.
        final ArrayList<Integer> workersAssigned = new ArrayList<>(instance.getStaffMemberCount());
        final ArrayList<Integer> workersNotAssigned = new ArrayList<>(instance.getStaffMemberCount());

        // Put staff members into the respective lists.
        for (int worker = 0; worker < instance.getStaffMemberCount(); worker++) {
            if (solution.getAssigned(worker, chosenDay)) {
                workersAssigned.add(worker);
            } else if (!instance.hasPTORequest(worker, chosenDay)) {
                // Staff member can only be feasibly reassigned if the is no corresponding PTO for the chosen day.
                workersNotAssigned.add(worker);
            }
        }

        if (workersAssigned.isEmpty() || workersNotAssigned.isEmpty()) {
            // Switching assignment between two staff members is infeasible. Return the original solution.
            return solution;
        }

        final BitMatrix newAssignments = solution.copyAssignments();
        final int unassigned = workersAssigned.get(random.nextInt(workersAssigned.size()));
        final int reassigned = workersNotAssigned.get(random.nextInt(workersNotAssigned.size()));
        newAssignments.set(unassigned, chosenDay, false);
        newAssignments.set(reassigned, chosenDay, true);
        return new ShiftSchedulingSolution(newAssignments);
    }

}
