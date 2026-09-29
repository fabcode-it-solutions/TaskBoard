package tuwien.taskinn.services.scheduling.solver;

import tuwien.taskinn.services.scheduling.instance.TaskSchedulingInstance;
import tuwien.taskinn.services.scheduling.solution.CostFunction;
import tuwien.taskinn.services.scheduling.solution.TaskSchedulingSolution;
import tuwien.taskinn.services.scheduling.solution.TaskSchedulingSolutionCostFunction;

import java.util.Random;

/**
 * A solver for task scheduling problems.
 */
public class TaskSchedulingSolver {

    private final int attempts;
    private final int restarts;
    private final int annealingIterations;
    private final Generator<TaskSchedulingSolution, TaskSchedulingInstance> generator;
    private final SimulatedAnnealing<Double, TaskSchedulingInstance, TaskSchedulingSolution> simulatedAnnealing;

    private final CostFunction<Double, TaskSchedulingInstance, TaskSchedulingSolution> costFunction =
            new TaskSchedulingSolutionCostFunction();

    /**
     * Constructs a new TaskSchedulingSolver with the given parameters.
     *
     * @param pAttempts number of tries starting from the seed solution
     * @param pRestarts number of individual simulated annealing runs on the same solution
     * @param pAnnealingIterations number of iterations for a single simulated annealing run
     * @param pGenerator generator for seed solutions
     * @param pNeighborhood neighborhood function for solutions
     */
    public TaskSchedulingSolver(int pAttempts, int pRestarts, int pAnnealingIterations,
            Generator<TaskSchedulingSolution, TaskSchedulingInstance> pGenerator,
            SolutionNeighborhood<TaskSchedulingSolution, TaskSchedulingInstance> pNeighborhood) {
        attempts = pAttempts;
        restarts = pRestarts;
        annealingIterations = pAnnealingIterations;
        generator = pGenerator;
        simulatedAnnealing = new SimulatedAnnealing<>(costFunction, pNeighborhood);
    }

    /**
     * Solves a given task scheduling problem.
     *
     * @param random source of randomness
     * @param instance the given task scheduling problem instance
     * @return a solution to the given scheduling problem instance
     */
    public TaskSchedulingSolution solve(Random random, TaskSchedulingInstance instance) {
        final TaskSchedulingSolution seedSolution = generator.generate(instance);
        TaskSchedulingSolution bestSolution = null;
        double bestCost = 0.0;

        for (int i = 0; i < attempts; i++) {
            TaskSchedulingSolution newSolution = seedSolution;
            for (int j = 0; j < restarts; j++) {
                newSolution = simulatedAnnealing.search(random, instance, newSolution, annealingIterations);
            }
            final double newCost = costFunction.cost(instance, newSolution);
            if (bestSolution == null || newCost < bestCost) {
                bestSolution = newSolution;
                bestCost = newCost;
            }
        }

        return bestSolution;
    }

}
