package tuwien.taskinn.services.scheduling.solution;

import lombok.RequiredArgsConstructor;
import tuwien.taskinn.services.scheduling.utility.BitMatrix;

/**
 * Represents a solution to a particular shift scheduling problem.
 */
@RequiredArgsConstructor
public class ShiftSchedulingSolution {

    private final BitMatrix assignments;

    /**
     * Returns whether the solution assigns a given staff member to work on a given day.
     *
     * @param staffMemberIndex the index of the given staff member
     * @param dayIndex the index of the given day
     * @return true if the solution assigns the given staff member to work on the given day, false otherwise
     */
    public boolean getAssigned(int staffMemberIndex, int dayIndex) {
        return assignments.get(staffMemberIndex, dayIndex);
    }

    /**
     * Returns a fresh copy of the BitMatrix underlying the solution.
     *
     * @return a fresh copy of the BitMatrix underlying the solution
     */
    public BitMatrix copyAssignments() {
        return assignments.copy();
    }

}
