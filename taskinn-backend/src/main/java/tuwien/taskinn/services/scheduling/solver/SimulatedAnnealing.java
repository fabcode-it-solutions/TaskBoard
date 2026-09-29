package tuwien.taskinn.services.scheduling.solver;

import java.util.Random;
import lombok.RequiredArgsConstructor;
import tuwien.taskinn.services.scheduling.instance.ProblemInstance;
import tuwien.taskinn.services.scheduling.solution.CostFunction;

/**
 * Generic implementation of simulated annealing to find a solution that minimizes a given cost function.
 *
 * @param <T> the type of cost values
 * @param <U> the type of problem instances
 * @param <V> the type of solution instances
 */
@RequiredArgsConstructor
public class SimulatedAnnealing<T, U extends ProblemInstance, V> {

    private final CostFunction<T, U, V> costFunction;
    private final SolutionNeighborhood<V, U> neighborhood;

    /**
     * Performs random annealing starting from a given initial solution.
     *
     * @param random source of randomness
     * @param instance the given problem instance
     * @param initialSolution the given initial solution
     * @param iterations number of iterations to run
     * @return the best solution encountered during the search
     */
    public V search(Random random, U instance, V initialSolution, int iterations) {
        V solution = initialSolution;
        V bestSolution = solution;

        T cost = costFunction.cost(instance, solution);
        T bestCost = cost;

        // Ensure that initial solution has a 80% acceptance probability.
        double temperature = costFunction.costDistance(cost) / Math.log(0.8);
        final double initialTemperature = temperature;

        for (int i = 0; i < iterations; i++) {
            V newSolution = neighborhood.randomNeighbor(random, instance, solution);
            T newCost = costFunction.cost(instance, newSolution);

            if (random.nextDouble() < acceptanceProbability(cost, newCost, temperature)) {
                solution = newSolution;
                cost = newCost;

                if (costFunction.compareCost(bestCost, newCost) > 0) {
                    bestSolution = solution;
                    bestCost = cost;
                }
            }

            // Update temperature: fast annealing.
            temperature = initialTemperature / (i + 2);
        }

        return bestSolution;
    }

    private double acceptanceProbability(T oldCost, T newCost, double temperature) {
        if (costFunction.compareCost(oldCost, newCost) > 0) {
            return 1.0;
        } else {
            // Metropolis criterion
            return Math.pow(Math.E, costFunction.costDistance(oldCost, newCost) / temperature);
        }
    }

}
