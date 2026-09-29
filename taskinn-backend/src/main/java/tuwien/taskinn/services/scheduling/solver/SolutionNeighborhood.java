package tuwien.taskinn.services.scheduling.solver;

import java.util.Random;
import tuwien.taskinn.services.scheduling.instance.ProblemInstance;

/**
 * A generic notion of a "neighborhood" for solutions of a given problem instance.
 *
 * @param <T> the type of solutions instances
 * @param <U> the type of problem instances
 */
public interface SolutionNeighborhood<T, U extends ProblemInstance> {

    /**
     * Returns a random neighbor for a given solution to the given instance.
     *
     * @param random source of randomness
     * @param instance the given problem instance
     * @param solution the given solution instance
     * @return a random neighbor of the solution
     */
    T randomNeighbor(Random random, U instance, T solution);

}
