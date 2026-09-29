package tuwien.taskinn.services.scheduling.solution;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;

/**
 * Represents a solution to a particular task scheduling problem.
 */
@RequiredArgsConstructor
public class TaskSchedulingSolution {

    private final ArrayList<ArrayList<Integer>> assignments;

    /**
     * Returns the number of rooms assigned to a given staff member.
     *
     * @param staffMemberIndex index of the given staff member
     * @return the number of rooms assigned to the given staff member
     */
    public int getAssignmentCount(int staffMemberIndex) {
        return assignments.get(staffMemberIndex).size();
    }

    /**
     * Returns the index of the room assigned to a given staff member at a particular position.
     *
     * @param staffMemberIndex index of the given staff member
     * @param position position of the given room within the given staff member's task list
     * @return the index of the room assigned to the given staff member at the position
     */
    public int getAssignments(int staffMemberIndex, int position) {
        return assignments.get(staffMemberIndex).get(position);
    }

    /**
     * Returns a fresh copy of the two-dimensional ArrayList underlying the solution.
     *
     * @return a fresh copy of the two-dimensional ArrayList underlying the solution
     */
    public ArrayList<ArrayList<Integer>> copyAssignments() {
        final ArrayList<ArrayList<Integer>> result = new ArrayList<>(assignments.size());
        for (final ArrayList<Integer> row : assignments) {
            final ArrayList<Integer> subresult = new ArrayList<>(row.size() + 1);
            subresult.addAll(row);
            result.add(subresult);
        }
        return result;
    }

}
