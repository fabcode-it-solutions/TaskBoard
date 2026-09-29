package tuwien.taskinn.services.scheduling.solution;

import tuwien.taskinn.services.scheduling.instance.ProblemInstance;

/**
 * A generic cost function for scoring solutions to problem instances of the given type.
 *
 * @param <T> type of the cost value
 * @param <U> type of the problem instance
 * @param <V> type of the solution instance
 */
public interface CostFunction<T, U extends ProblemInstance, V> {

    /**
     * Calculates the cost for a given solution to a given problem instance.
     *
     * @param instance the given problem instance
     * @param solution the given solution
     * @return the cost for the given solution to the given problem instance
     */
    T cost(U instance, V solution);

    /**
     * Compares two cost values to each other.
     *
     * @param left the left cost value
     * @param right the right cost value
     * @return a value less than 0 if left is less than right, a value greater than 0 if left is greater than right and
     *         0 otherwise
     */
    int compareCost(T left, T right);

    /**
     * Computes a distance value from a given cost to the cost of the empty solution.
     *
     * @param cost the given cost
     * @return a distance value between the given cost and the cost of the empty solution
     */
    double costDistance(T cost);

    /**
     * Computes a distance value between the given cost values.
     *
     * @param left the first cost value
     * @param right the second cost value
     * @return a distance value between the given cost values
     */
    double costDistance(T left, T right);

}
