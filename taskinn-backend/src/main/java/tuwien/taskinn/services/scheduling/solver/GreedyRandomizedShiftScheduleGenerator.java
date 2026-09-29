package tuwien.taskinn.services.scheduling.solver;

import java.util.Random;
import lombok.RequiredArgsConstructor;
import tuwien.taskinn.exceptions.InfeasibleInstanceException;
import tuwien.taskinn.services.scheduling.instance.ShiftSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.ShiftSchedulingSolution;
import tuwien.taskinn.services.scheduling.utility.BitMatrix;
import tuwien.taskinn.services.scheduling.utility.Stochastic;

/**
 * A concrete generator for solutions to a particular shift scheduling problem. It randomly chooses assignments,
 * weighted by the local cost (the greedy component).
 */
@RequiredArgsConstructor
public class GreedyRandomizedShiftScheduleGenerator
        implements Generator<ShiftSchedulingSolution, ShiftSchedulingInstance> {

    final private Random random;

    @Override
    public ShiftSchedulingSolution generate(ShiftSchedulingInstance instance) {
        final BitMatrix assignment = new BitMatrix(instance.getStaffMemberCount(), instance.getDayCount());
        // To keep track of every staff member's work hours up to this point.
        final int[] workerHours = new int[instance.getStaffMemberCount()];

        // Inputs to the random weighted selection. To be filled for every day.
        final Integer[] proposalWorker = new Integer[instance.getStaffMemberCount()];
        final float[] proposalWeight = new float[instance.getStaffMemberCount()];

        for (int day = 0; day < instance.getDayCount(); day++) {
            int staffingLevel = instance.getStaffingLevel(day);
            int dayHours = instance.getDayHours(day);
            int workersAvailable = 0;

            for (int worker = 0; worker < instance.getStaffMemberCount(); worker++) {
                if (instance.isWeekStart(day)) {
                    // Weekly work hours reset at the start of a new week
                    workerHours[worker] = 0;
                }

                if (instance.hasPTORequest(worker, day)) {
                    // Skip all staff members with PTO on the given day.
                    continue;
                }
                // Calculate salary for staff member up to this point plus for today. Add the worker to the selection
                // pool, weighted by the reciprocal of the salary costs.
                final int newHours = workerHours[worker] + dayHours;
                final var workerData = instance.getStaffMember(worker);
                final int cost = Math.min(newHours, workerData.targetHours()) * workerData.normalPay()
                        + Math.max(0, newHours - workerData.targetHours()) * workerData.overtimePay();
                proposalWorker[workersAvailable] = worker;
                proposalWeight[workersAvailable] = 1.0f / ((float) cost);
                workersAvailable++;
            }
            if (workersAvailable < staffingLevel) {
                throw new InfeasibleInstanceException("Too few staff members");
            }
            final Integer[] chosen = new Integer[staffingLevel];
            Stochastic.weightedChoices(random, proposalWorker, workersAvailable, chosen, proposalWeight);
            for (int worker : chosen) {
                // Update assignment and work hours for every chosen worker.
                workerHours[worker] += dayHours;
                assignment.set(worker, day, true);
            }
        }

        return new ShiftSchedulingSolution(assignment);
    }

    @Override
    public boolean isDeterministic() {
        return false;
    }

}
