package tuwien.taskinn.services.scheduling.solution;

import tuwien.taskinn.services.scheduling.instance.ShiftSchedulingInstance;

/**
 * A concrete cost function for shift scheduling problems.
 */
public class ShiftSchedulingSolutionCostFunction
        implements CostFunction<Integer, ShiftSchedulingInstance, ShiftSchedulingSolution> {

    @Override
    public Integer cost(ShiftSchedulingInstance instance, ShiftSchedulingSolution solution) {
        int cost = 0;

        // Sum up the amount of salary paid to all employees under the given solution and return it.
        for (int worker = 0; worker < instance.getStaffMemberCount(); worker++) {
            var workerData = instance.getStaffMember(worker);
            int workerHours = 0;
            for (int day = 0; day < instance.getDayCount(); day++) {
                if (instance.isWeekStart(day)) {
                    // Weekly work hours reset at the start of a new week
                    workerHours = 0;
                }

                if (solution.getAssigned(worker, day)) {
                    workerHours += instance.getDayHours(day);
                }
            }
            cost += Math.min(workerHours, workerData.targetHours()) * workerData.normalPay()
                    + Math.max(0, workerHours - workerData.targetHours()) * workerData.overtimePay();
        }

        return cost;
    }

    @Override
    public int compareCost(Integer left, Integer right) {
        return left.compareTo(right);
    }

    @Override
    public double costDistance(Integer score) {
        return score.doubleValue();
    }

    @Override
    public double costDistance(Integer left, Integer right) {
        return left.doubleValue() - right.doubleValue();
    }

}
