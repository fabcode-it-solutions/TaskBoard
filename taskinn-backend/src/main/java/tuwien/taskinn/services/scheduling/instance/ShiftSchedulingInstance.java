package tuwien.taskinn.services.scheduling.instance;

import java.util.List;
import java.util.stream.Stream;
import tuwien.taskinn.services.scheduling.utility.BitMatrix;

/**
 * Representation of a concrete shift scheduling problem instance. Describes a number of staff members that need to be
 * scheduled for a given number of days with the given scheduling constraints.
 */
public class ShiftSchedulingInstance implements ProblemInstance {

    /**
     * Represents all the information that is needed for a single staff member.
     *
     * @param targetHours the weekly target hours
     * @param normalPay the amount of pay for work performed within the target hours
     * @param overtimePay the amount of pay for work performed above the target hours
     * @param ptoRequests a list of days (represented by indices) for which PTO has been requested
     */
    public record StaffMember(int targetHours, int normalPay, int overtimePay, List<Integer> ptoRequests) {
    }

    private final List<StaffMember> staffMembers;
    private final List<Integer> staffingLevels;
    private final List<Integer> dailyHours;
    private final List<Boolean> weekStarts;
    private final BitMatrix ptoRequests;

    /**
     * Constructs a ShiftSchedulingInstance with the given staff members, staffing levels and daily working hours.
     *
     * @param pStaffMembers the given staff members
     * @param pStaffingLevels the given staffing levels
     * @param pDailyHours the given daily working hours
     * @param pWeekStarts determines whether a given day is considered the start of a week
     */
    public ShiftSchedulingInstance(Stream<StaffMember> pStaffMembers, Stream<Integer> pStaffingLevels,
            Stream<Integer> pDailyHours, Stream<Boolean> pWeekStarts) {
        staffMembers = pStaffMembers.toList();
        if (staffMembers.isEmpty()) {
            throw new IllegalArgumentException("No staff members given");
        }
        staffingLevels = pStaffingLevels.toList();
        if (staffingLevels.isEmpty()) {
            throw new IllegalArgumentException("No staffing levels given");
        }
        dailyHours = pDailyHours.toList();
        weekStarts = pWeekStarts.toList();
        if (dailyHours.isEmpty()) {
            throw new IllegalArgumentException("No daily hours given");
        }
        if (staffingLevels.size() != dailyHours.size()) {
            throw new IllegalArgumentException("Staffing levels and daily hours have different dimensions");
        }
        if (weekStarts.isEmpty()) {
            throw new IllegalArgumentException("No week starts given");
        }
        if (dailyHours.size() != weekStarts.size()) {
            throw new IllegalArgumentException("Daily hours and week starts have different dimensions");
        }
        ptoRequests = new BitMatrix(staffMembers.size(), dailyHours.size());

        for (int level : staffingLevels) {
            if (level < 0) {
                throw new IllegalArgumentException("Negative staffing level");
            }
        }

        for (int hours : dailyHours) {
            if (hours < 0) {
                throw new IllegalArgumentException("Negative hours");
            }
        }

        // Fill out the matrix of PTO requests
        for (int i = 0; i < staffMembers.size(); i++) {
            for (int dayIndex : staffMembers.get(i).ptoRequests) {
                if (dayIndex < 0 || dayIndex >= dailyHours.size())
                    throw new IllegalArgumentException("PTO day index out of range");
                ptoRequests.set(i, dayIndex, true);
            }
        }
    }

    /**
     * Returns information for the staff member at the given index.
     *
     * @param index the given index
     * @return staff member information
     */
    public StaffMember getStaffMember(int index) {
        return staffMembers.get(index);
    }

    /**
     * Returns the required staffing level for the day at the given index.
     *
     * @param index the given index
     * @return the required staffing level
     */
    public int getStaffingLevel(int index) {
        return staffingLevels.get(index);
    }

    /**
     * Returns the number of work hours for the day at the given index.
     *
     * @param index the given index
     * @return the number of work hours
     */
    public int getDayHours(int index) {
        return dailyHours.get(index);
    }

    /**
     * Returns whether a given staff member has a PTO request on the given day.
     *
     * @param staffMemberIndex the index of the given staff member
     * @param dayIndex the index of the given day
     * @return true if the given staff member has a PTO request on the given day, false otherwise
     */
    public boolean hasPTORequest(int staffMemberIndex, int dayIndex) {
        return ptoRequests.get(staffMemberIndex, dayIndex);
    }

    /**
     * Returns whether the given day is the start of a week
     *
     * @param dayIndex index of the given day
     * @return whether the given day is the start of a week
     */
    public boolean isWeekStart(int dayIndex) {
        return weekStarts.get(dayIndex);
    }

    /**
     * Returns the number of staff members in the instance.
     *
     * @return the number of staff members in the instance
     */
    public int getStaffMemberCount() {
        return staffMembers.size();
    }

    /**
     * Returns the number of days in the instance.
     *
     * @return the number of days in the instance
     */
    public int getDayCount() {
        return dailyHours.size();
    }

    @Override
    public boolean isInfeasible() {
        // For every day, sum up all staff members not having a PTO request. If the total is less than the required
        // staffing level, the instance is infeasible.
        for (int day = 0; day < dailyHours.size(); day++) {
            int available = 0;
            for (int staff = 0; staff < staffMembers.size(); staff++) {
                if (!ptoRequests.get(staff, day)) {
                    available += 1;
                }
            }
            if (available < staffingLevels.get(day)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long instanceSeed() {
        // TODO: Make instance specific?
        return 0xBEEFL;
    }

}
