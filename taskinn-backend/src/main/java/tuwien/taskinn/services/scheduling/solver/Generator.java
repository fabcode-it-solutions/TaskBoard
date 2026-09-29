package tuwien.taskinn.services.scheduling.solver;

import tuwien.taskinn.services.scheduling.instance.ProblemInstance;

/**
 * A generic generator for producing solutions to a given problem instance.
 *
 * @param <T> the type of the solution instance
 * @param <U> the type of the problem instance
 */
public interface Generator<T, U extends ProblemInstance> {

    /**
     * Generates a solution to the given problem instance.
     *
     * @param instance the given problem instance
     * @return a solution to the given problem instance
     */
    T generate(U instance);

    /**
     * Returns whether the generator is deterministic, meaning it will always return the same solution for the same call
     * to generate.
     *
     * @return true if the generator is deterministic, false otherwise
     */
    boolean isDeterministic();

}
