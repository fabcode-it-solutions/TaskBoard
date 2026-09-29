package tuwien.taskinn.services.scheduling.instance;

/**
 * A generic representation of a scheduling problem instance.
 */
public interface ProblemInstance {

    /**
     * Returns whether the instance is judged to be infeasible. The implemented check must be sound (e.g. it may not
     * return true for feasible instances), but not necessarily complete (e.g. it may return false for infeasible
     * instances).
     *
     * @return whether the instance is judged to be infeasible
     */
    boolean isInfeasible();

    /**
     * Return an instance-specific seed value for use in random number generation. Should be the same for equal
     * instances in order to ensure consistent results
     *
     * @return an instance-specific seed value
     */
    long instanceSeed();

}
