package tuwien.taskinn.services.scheduling.utility;

import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * Implementation of a max heap storing elements of a given type together with an integer weight.
 *
 * @param <T> the type of heap elements
 */
public class MaxHeap<T> {

    private record Node<T>(int weight, T value) {
    }

    private final ArrayList<Node<T>> tree;

    /**
     * Constructs a new MaxHeap.
     */
    public MaxHeap() {
        this(32);
    }

    /**
     * Constructs a new MaxHeap with a given initial capacity.
     *
     * @param initialCapacity given initial capacity
     */
    public MaxHeap(int initialCapacity) {
        tree = new ArrayList<>(initialCapacity);
    }

    /**
     * Returns the number of elements currently contained in the heap.
     *
     * @return the number of elements currently contained in the heap
     */
    public int size() {
        return tree.size();
    }

    /**
     * Inserts a new element with the given weight into the heap.
     *
     * @param weight the given weight
     * @param value the given element
     */
    public void insert(int weight, T value) {
        int i = tree.size();
        tree.add(new Node<>(weight, value));
        // Heapify up
        while (i > 0) {
            final int parent = nodeParent(i);
            if (nodeWeight(i) < nodeWeight(parent)) {
                break;
            }
            swapNodes(i, parent);
            i = parent;
        }
    }

    /**
     * Returns the heap element with the largest weight, removing it from the heap while maintaining the heap invariant.
     *
     * @return the heap element with the largest weight
     */
    public T extractMax() {
        if (tree.isEmpty()) {
            throw new NoSuchElementException();
        }
        final T result = tree.getFirst().value;
        if (tree.size() == 1) {
            tree.removeLast();
        } else {
            tree.set(0, tree.removeLast());
            heapifyDown(0);
        }
        return result;
    }

    private void heapifyDown(int x) {
        final int left = nodeLeft(x);
        final int right = nodeRight(x);
        int max = x;
        if (left < tree.size() && nodeWeight(left) > nodeWeight(max))
            max = left;
        if (right < tree.size() && nodeWeight(right) > nodeWeight(max))
            max = right;
        if (max != x) {
            swapNodes(x, max);
            heapifyDown(max);
        }
    }

    private int nodeParent(int x) {
        return (x - 1) / 2;
    }

    private int nodeLeft(int x) {
        return (2 * x) + 1;
    }

    private int nodeRight(int x) {
        return (2 * x) + 2;
    }

    private int nodeWeight(int x) {
        return tree.get(x).weight;
    }

    private void swapNodes(int x, int y) {
        final Node<T> tmp = tree.get(x);
        tree.set(x, tree.get(y));
        tree.set(y, tmp);
    }

}
