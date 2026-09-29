package tuwien.taskinn.services.scheduling.utility;

import java.util.Arrays;
import java.util.Random;

public class Stochastic {

    /**
     * Performs random, weighted selection of a given number of elements without replacement. Considers only the first N
     * elements of the input array. The number of elements to select is determined by the results array.
     *
     * @param random source of randomness
     * @param elements input elements to perform selection on
     * @param N number of elements in the input array (starting from the beginning) to consider
     * @param results array to place the results into
     * @param weights weights of the input elements
     * @param <T> type of the input elements
     */
    public static <T> void weightedChoices(Random random, T[] elements, int N, T[] results, float[] weights) {
        if (elements.length != weights.length) {
            throw new IllegalArgumentException("Elements and weights have different dimensions");
        }
        if (N < 0) {
            throw new IllegalArgumentException("Negative N");
        }
        if (N > elements.length) {
            throw new IllegalArgumentException("N greater than number of elements");
        }
        if (results.length > N) {
            throw new IllegalArgumentException("Not enough elements");
        }

        if (N == 0 || results.length == 0) {
            return;
        }

        final T[] elementsCopy = Arrays.copyOf(elements, N);
        final float[] weightsCopy = Arrays.copyOf(weights, N);

        final float[] weightPrefixSum = new float[N];
        float prefixSum = 0.0f;
        for (int i = 0; i < weightsCopy.length; i++) {
            if (weightsCopy[i] < 0.0f) {
                throw new IllegalArgumentException("Negative weight");
            }
            prefixSum += weightsCopy[i];
            weightPrefixSum[i] = prefixSum;
        }
        if (prefixSum == 0.0f) {
            throw new IllegalStateException("No elements with non-zero weight remaining");
        }

        int inserted = 0;
        int remaining = elementsCopy.length;

        while (inserted != results.length) {
            final float decider = random.nextFloat(prefixSum);
            // Use the prefix sum to perform weighted selection
            final int searchIndex = Arrays.binarySearch(weightPrefixSum, 0, remaining, decider);
            final int index = (searchIndex >= 0) ? searchIndex : -(searchIndex + 1);
            results[inserted++] = elementsCopy[index];

            if ((index + 1) < remaining) {
                // Remove the selected element by moving the last element of the input into its position and decreasing
                // the input size by 1
                elementsCopy[index] = elementsCopy[remaining - 1];
                weightsCopy[index] = weightsCopy[remaining - 1];

                // The prefix sums are now invalid starting from the given index. Repair them
                prefixSum = (index == 0) ? 0.0f : weightsCopy[index - 1];
                for (int i = index; i < remaining; i++) {
                    prefixSum += weightsCopy[i];
                    weightPrefixSum[i] = prefixSum;
                }
            } else {
                // Just decrease the total sum of weights, since the removed element was the last one anyway
                prefixSum -= weightsCopy[remaining - 1];
            }

            if (inserted < results.length && prefixSum == 0.0f) {
                throw new IllegalStateException("No elements with non-zero weight remaining");
            }

            remaining--;
        }
    }

}
