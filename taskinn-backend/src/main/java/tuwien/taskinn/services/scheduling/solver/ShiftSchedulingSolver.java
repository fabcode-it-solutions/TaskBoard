package tuwien.taskinn.services.scheduling.solver;

import java.util.Random;
import tuwien.taskinn.exceptions.InfeasibleInstanceException;
import tuwien.taskinn.services.scheduling.instance.ShiftSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.CostFunction;
import tuwien.taskinn.services.scheduling.solution.ShiftSchedulingSolution;
import tuwien.taskinn.services.scheduling.solution.ShiftSchedulingSolutionCostFunction;

/**
 * A solver for shift scheduling problems.
 */
public class ShiftSchedulingSolver {

    private final int attempts;
    private final int restarts;
    private final int annealingIterations;
    private final Generator<ShiftSchedulingSolution, ShiftSchedulingInstance> generator;
    private final SimulatedAnnealing<Integer, ShiftSchedulingInstance, ShiftSchedulingSolution> simulatedAnnealing;

    private final CostFunction<Integer, ShiftSchedulingInstance, ShiftSchedulingSolution> costFunction =
            new ShiftSchedulingSolutionCostFunction();

    /**
     * Constructs a new ShiftSchedulingSolver with the given parameters.
     *
     * @param pAttempts number of tries with a new seed solution (only relevant for nondeterministic generators,
     *        otherwise set to 1)
     * @param pRestarts number of individual simulated annealing runs on the same solution
     * @param pAnnealingIterations number of iterations for a single simulated annealing run
     * @param pGenerator generator for seed solutions
     * @param pNeighborhood neighborhood function for solutions
     */
    public ShiftSchedulingSolver(int pAttempts, int pRestarts, int pAnnealingIterations,
            Generator<ShiftSchedulingSolution, ShiftSchedulingInstance> pGenerator,
            SolutionNeighborhood<ShiftSchedulingSolution, ShiftSchedulingInstance> pNeighborhood) {
        attempts = pAttempts;
        restarts = pRestarts;
        annealingIterations = pAnnealingIterations;
        generator = pGenerator;
        simulatedAnnealing = new SimulatedAnnealing<>(costFunction, pNeighborhood);
    }

    /**
     * Solves a given shift scheduling problem.
     *
     * @param random source of randomness
     * @param instance the given shift scheduling problem instance
     * @return a solution to the given shift scheduling problem instance
     */
    public ShiftSchedulingSolution solve(Random random, ShiftSchedulingInstance instance) {
        if (instance.isInfeasible()) {
            throw new InfeasibleInstanceException();
        }

        ShiftSchedulingSolution bestSolution = null;
        int bestCost = 0;

        ShiftSchedulingSolution seedSolution = null;
        for (int i = 0; i < attempts; i++) {
            if (seedSolution == null || !generator.isDeterministic()) {
                seedSolution = generator.generate(instance);
            }
            ShiftSchedulingSolution newSolution = seedSolution;
            for (int j = 0; j < restarts; j++) {
                newSolution = simulatedAnnealing.search(random, instance, newSolution, annealingIterations);
            }
            int newCost = costFunction.cost(instance, newSolution);
            if (bestSolution == null || newCost < bestCost) {
                bestSolution = newSolution;
                bestCost = newCost;
            }
        }

        return bestSolution;
    }

}
