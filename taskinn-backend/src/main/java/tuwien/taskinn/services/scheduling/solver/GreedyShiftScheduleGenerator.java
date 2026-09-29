package tuwien.taskinn.services.scheduling.solver;

import tuwien.taskinn.exceptions.InfeasibleInstanceException;
import tuwien.taskinn.services.scheduling.instance.ShiftSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.ShiftSchedulingSolution;
import tuwien.taskinn.services.scheduling.utility.BitMatrix;
import tuwien.taskinn.services.scheduling.utility.MaxHeap;

/**
 * A concrete generator for solutions to a particular shift scheduling problem. It greedily chooses the cheapest
 * assignments at every step.
 */
public class GreedyShiftScheduleGenerator implements Generator<ShiftSchedulingSolution, ShiftSchedulingInstance> {

    @Override
    public ShiftSchedulingSolution generate(ShiftSchedulingInstance instance) {
        final BitMatrix assignment = new BitMatrix(instance.getStaffMemberCount(), instance.getDayCount());
        // To keep track of every staff member's work hours up to this point.
        final int[] workerHours = new int[instance.getStaffMemberCount()];

        for (int day = 0; day < instance.getDayCount(); day++) {
            int staffingLevel = instance.getStaffingLevel(day);
            int dayHours = instance.getDayHours(day);
            MaxHeap<Integer> proposals = new MaxHeap<>(staffingLevel);
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
                // pool, weighted by the negated salary costs (since proposals is a MaxHeap).
                final int newHours = workerHours[worker] + dayHours;
                final var workerData = instance.getStaffMember(worker);
                final int cost = Math.min(newHours, workerData.targetHours()) * workerData.normalPay()
                        + Math.max(0, newHours - workerData.targetHours()) * workerData.overtimePay();
                proposals.insert(-cost, worker);
            }
            if (proposals.size() < staffingLevel) {
                throw new InfeasibleInstanceException("Too few staff members");
            }
            for (int i = 0; i < staffingLevel; i++) {
                // Update assignment and work hours for every chosen worker.
                int worker = proposals.extractMax();
                workerHours[worker] += dayHours;
                assignment.set(worker, day, true);
            }
        }

        return new ShiftSchedulingSolution(assignment);
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

}
