package tuwien.taskinn.services.scheduling.solver;

import tuwien.taskinn.services.scheduling.instance.TaskSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.TaskSchedulingSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A concrete, greedy generator for solutions to a particular task scheduling problem.
 */
public class GreedyTaskScheduleGenerator implements Generator<TaskSchedulingSolution, TaskSchedulingInstance> {

    @Override
    public TaskSchedulingSolution generate(TaskSchedulingInstance instance) {
        // Tracks which rooms are already included in the solution.
        final boolean[] visited = new boolean[instance.getRoomCount()];
        // Tracks how many room tasks are unconstrained.
        int nonConstrainedCount = 0;
        // List of room tasks with constraints on their end.
        final ArrayList<Integer> endConstrainedRooms = new ArrayList<>();
        // List of room tasks with constraints on their start.
        final ArrayList<Integer> startConstrainedRooms = new ArrayList<>();
        // Tracks which rooms have constraints on them.
        final boolean[] roomConstrained = new boolean[instance.getRoomCount()];

        // Generate an initial route with all unconstrained rooms.

        // Tracks the necessary time for all assigned rooms.
        double assignedTime = 0;
        // The initial route.
        final ArrayList<Integer> route = new ArrayList<>(instance.getRoomCount());
        // Categorize room tasks and initialize route.
        for (int i = 0; i < instance.getRoomCount(); i++) {
            final var room = instance.getRoom(i);
            // If room task is constrained, record it and move on.
            if (room.maxEndTime() >= 0 && room.minStartTime() >= 0) {
                throw new IllegalArgumentException("Constraining both start and end time is not supported");
            } else if (room.maxEndTime() >= 0) {
                endConstrainedRooms.add(i);
                roomConstrained[i] = true;
                continue;
            } else if (room.minStartTime() >= 0) {
                startConstrainedRooms.add(i);
                roomConstrained[i] = true;
                continue;
            }

            // The first non-constrained room that is encountered is taken as the starting point.
            if (route.isEmpty()) {
                route.add(i);
                visited[i] = true;
                assignedTime = room.taskDuration();
            } else {
                nonConstrainedCount++;
            }
        }
        // Build up route by greedily choosing the next closest point.
        for (int i = 0; i < nonConstrainedCount; i++) {
            final int previous = route.getLast();
            final int next = argmin(instance.getRoomCount(), x -> instance.getRoomDistance(previous, x),
                    x -> (x != previous) && !visited[x] && !roomConstrained[x]);
            if (next < 0) {
                throw new RuntimeException("could not find next in greedy construction");
            }
            assignedTime += instance.getRoomDistance(previous, next) + instance.getRoom(next).taskDuration();
            visited[next] = true;
            route.add(next);
        }

        // Average time per staff member.
        final double averageAssignedTime = assignedTime / (double) instance.getStaffMemberCount();

        // Divide route up for all the staff members.

        // Route for the current staff member.
        ArrayList<Integer> currentRoute = new ArrayList<>();
        // Index of the current staff member.
        int currentStaffMember = 0;
        // Current time taken for the current staff member.
        double currentTime = 0.0;
        // List of all routes.
        final ArrayList<ArrayList<Integer>> allRoutes = new ArrayList<>();
        // Tracks the number of added routes that were empty.
        int nEmptyRoutes = 0;
        // Associates rooms with the staff members assigned to them.
        final int[] roomStaffAssignments = new int[instance.getRoomCount()];
        Arrays.fill(roomStaffAssignments, -1);
        // Associates staff members with the amount of time they are scheduled for.
        final double[] staffAssignedTimes = new double[instance.getStaffMemberCount()];
        for (final int roomIndex : route) {
            final double deltaTime = instance.getRoom(roomIndex).taskDuration()
                    + (currentRoute.isEmpty() ? 0.0 : instance.getRoomDistance(currentRoute.getLast(), roomIndex));
            final double previousCost = Math.abs(averageAssignedTime - currentTime);
            final double currentCost = Math.abs(averageAssignedTime - (currentTime + deltaTime));
            // Add to the current employee's route only if doing so would bring the assigned time closer to the
            // theoretical optimum.
            // If not, move on to the next staff member.
            if (currentCost > previousCost && allRoutes.size() < instance.getStaffMemberCount()
                    && (currentStaffMember + 1) < instance.getStaffMemberCount()) {
                allRoutes.add(currentRoute);
                if (currentRoute.isEmpty()) {
                    nEmptyRoutes++;
                }
                currentRoute = new ArrayList<>();
                currentStaffMember++;
                currentTime = 0.0;
            }
            currentRoute.add(roomIndex);
            roomStaffAssignments[roomIndex] = currentStaffMember;
            currentTime += deltaTime;
            staffAssignedTimes[currentStaffMember] = currentTime;
        }
        // If the last route is not empty then also add it to the other routes.
        if (!currentRoute.isEmpty()) {
            if (allRoutes.size() < instance.getStaffMemberCount()) {
                allRoutes.add(currentRoute);
                staffAssignedTimes[currentStaffMember] = currentTime;
            } else {
                allRoutes.getLast().addAll(currentRoute);
                staffAssignedTimes[instance.getStaffMemberCount() - 1] += currentTime;
            }
        }
        // Fill the list of all routes with empty routes until it is equal in size to the number of staff members.
        while (allRoutes.size() < instance.getStaffMemberCount()) {
            allRoutes.add(new ArrayList<>());
            nEmptyRoutes++;
        }

        // Sort the room tasks with end constraints by their starting time.
        endConstrainedRooms.sort((l, r) -> {
            final var roomL = instance.getRoom(l);
            final var roomR = instance.getRoom(r);
            final int startOrder = Integer.compare(roomL.maxEndTime() - roomL.taskDuration(),
                    roomR.maxEndTime() - roomR.taskDuration());
            if (startOrder == 0) {
                return Integer.compare(roomL.maxEndTime(), roomR.maxEndTime());
            } else {
                return startOrder;
            }
        });
        // Keeps track of the latest end time of end constrained room tasks per employee.
        final int[] endFrontiers = new int[instance.getStaffMemberCount()];
        // Keeps track of how many end constrained rooms tasks were assigned to each employee.
        final int[] insertedEndConstrained = new int[instance.getStaffMemberCount()];
        for (final int roomIndex : endConstrainedRooms) {
            // If empty routes exists, just add the room there.
            if (nEmptyRoutes > 0) {
                for (int i = 0; i < instance.getStaffMemberCount(); i++) {
                    if (allRoutes.get(i).isEmpty()) {
                        allRoutes.get(i).add(roomIndex);
                        insertedEndConstrained[roomIndex]++;
                        roomStaffAssignments[roomIndex] = i;
                        nEmptyRoutes--;
                        break;
                    }
                }
                if (roomStaffAssignments[roomIndex] >= 0) {
                    continue;
                }
            }

            final var room = instance.getRoom(roomIndex);
            // Try to find the closest staff member that can still take the room task without violating the constraint.
            int bestStaffMember = -1;
            double bestDistance = Double.MAX_VALUE;
            for (int i = 0; i < instance.getRoomCount(); i++) {
                final int staffMember = roomStaffAssignments[i];
                if (staffMember < 0 || i == roomIndex
                        || (endFrontiers[staffMember] + room.taskDuration()) > room.maxEndTime()) {
                    continue;
                }
                final double distance = instance.getRoomDistance(roomIndex, i);
                if (bestStaffMember < 0 || distance < bestDistance) {
                    bestStaffMember = staffMember;
                    bestDistance = distance;
                }
            }
            // If the previous failed, just find the staff member with the minimal distance from the maximal end time
            if (bestStaffMember < 0) {
                bestStaffMember =
                        argmin(instance.getStaffMemberCount(), x -> (double) (endFrontiers[x] - room.maxEndTime()));
            }
            if (bestStaffMember < 0) {
                throw new RuntimeException("Could not assign any staff member");
            }
            allRoutes.get(bestStaffMember).add(insertedEndConstrained[bestStaffMember], roomIndex);
            roomStaffAssignments[roomIndex] = bestStaffMember;
            endFrontiers[bestStaffMember] = endFrontiers[bestStaffMember] + room.taskDuration();
            final double distanceToPrevious = (insertedEndConstrained[bestStaffMember] > 0) ? instance.getRoomDistance(
                    allRoutes.get(bestStaffMember).get(insertedEndConstrained[bestStaffMember]), roomIndex) : 0;
            staffAssignedTimes[bestStaffMember] += distanceToPrevious + room.taskDuration();
            insertedEndConstrained[bestStaffMember]++;
        }

        // Sort the room tasks with start constraints by their end times.
        startConstrainedRooms.sort((l, r) -> {
            final var roomL = instance.getRoom(l);
            final var roomR = instance.getRoom(r);
            final int startOrder = Integer.compare(roomL.minStartTime(), roomR.minStartTime());
            if (startOrder == 0) {
                return Integer.compare(roomL.minStartTime() + roomL.taskDuration(),
                        roomR.minStartTime() + roomR.taskDuration());
            } else {
                return startOrder;
            }
        });
        // Assign the start constrained room tasks to the staff member with the least amount of assigned time.
        for (final int roomIndex : startConstrainedRooms) {
            final int bestStaffMember = argmin(instance.getStaffMemberCount(), x -> staffAssignedTimes[x]);
            if (bestStaffMember < 0) {
                throw new RuntimeException("could not assign any staff member");
            }
            allRoutes.get(bestStaffMember).add(roomIndex);
            staffAssignedTimes[bestStaffMember] += instance.getRoom(roomIndex).taskDuration();
            // If something is added after this, then all per staff/room state has to be maintained here
        }

        return new TaskSchedulingSolution(allRoutes);
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

    /**
     * Evaluates a given cost function over an index set of a given size. Returns an index that maximizes the given cost
     * function over the index set. If no such index exists, returns a negative number.
     *
     * @param size the size of the given index set
     * @param cost the given cost function
     * @return an index maximizing the given cost function or a negative number if no such index exists
     */
    private int argmin(int size, Function<Integer, Double> cost) {
        return argmin(size, cost, x -> true);
    }

    /**
     * Evaluates a given cost function over an index set of a given size. Returns an index that maximizes the given cost
     * function over the index set while also satisfying a given guard condition. If no such index exists, returns a
     * negative number.
     *
     * @param size the size of the given index set
     * @param cost the given cost function
     * @param guard the given guard condition
     * @return an index maximizing the given cost function while satisfying the given guard condition or a negative
     *         number if no such index exists
     */
    private int argmin(int size, Function<Integer, Double> cost, Predicate<Integer> guard) {
        int result = -1;
        double bestCost = 0.0;

        for (int i = 0; i < size; i++) {
            if (!guard.test(i)) {
                continue;
            }
            final double newCost = cost.apply(i);
            if (result == -1 || newCost < bestCost) {
                result = i;
                bestCost = newCost;
            }
        }

        return result;
    }

}
