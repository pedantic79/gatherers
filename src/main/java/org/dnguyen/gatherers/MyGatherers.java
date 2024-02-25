package org.dnguyen.gatherers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SequencedCollection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Gatherer;
import java.util.stream.Gatherer.Downstream;
import java.util.stream.Gatherer.Integrator;
import java.util.stream.Stream;

public class MyGatherers {

    public static <T> Gatherer<T, ?, T> doNothing() {
        Integrator.Greedy<Void, T, T> integrator = (_, element, downstream) -> {
            downstream.push(element);
            return true;
        };

        return Gatherer.of(integrator);
    }

    public static <T> Gatherer<T, ?, T> inspect(Consumer<? super T> inspector) {
        Integrator.Greedy<Void, T, T> integrator = (_, element, downstream) -> {
            inspector.accept(element);
            downstream.push(element);
            return true;
        };

        return Gatherer.of(integrator);
    }

    public static <T, R> Gatherer<T, ?, R> map(
            Function<? super T, ? extends R> mapper) {
        Integrator.Greedy<Void, T, R> integrator = (_, element, downstream) -> {
            R newElem = mapper.apply(element);
            downstream.push(newElem);
            return true;
        };

        return Gatherer.of(integrator);
    }

    public static <T> Gatherer<T, ?, T> filter(Predicate<? super T> predicate) {
        Integrator.Greedy<Void, T, T> integrator = (_, element, downstream) -> {
            if (predicate.test(element)) {
                downstream.push(element);
            }
            return true;
        };
        return Gatherer.of(integrator);
    }

    public static <T> Gatherer<T, ?, T> flatMapIf(
            Predicate<? super T> predicate,
            Function<? super T, Stream<? extends T>> mapper) {
        Integrator.Greedy<Void, T, T> integrator = (_, element, downstream) -> {
            if (predicate.test(element)) {
                mapper.apply(element).forEach(downstream::push);
            } else {
                downstream.push(element);
            }
            return true;
        };
        return Gatherer.of(integrator);
    }

    public static <T> Gatherer<T, ?, T> takeWhileIncluding(
            Predicate<? super T> predicate) {
        Integrator<Void, T, T> integrator = (_, element, downstream) -> {
            downstream.push(element);
            return predicate.test(element);
        };
        return Gatherer.ofSequential(integrator);
    }

    public static <T> Gatherer<T, ?, T> limit(int n) {
        Supplier<AtomicInteger> initializer = AtomicInteger::new;
        Integrator<AtomicInteger, T, T> integrator = (state, element, downstream) -> {
            int current = state.incrementAndGet();
            downstream.push(element);
            return current < n;
        };

        return Gatherer.ofSequential(initializer, integrator);
    }

    public static <T> Gatherer<T, ?, T> increasing(Comparator<? super T> comparator) {
        Supplier<AtomicReference<T>> initializer = AtomicReference::new;
        Integrator.Greedy<AtomicReference<T>, T, T> integrator
                = (state, element, downstream) -> {
                    T last = state.get();
                    if (last == null || comparator.compare(last, element) < 0) {
                        downstream.push(element);
                        state.set(element);
                    }

                    return true;
                };

        return Gatherer.ofSequential(initializer, integrator);
    }

    public static <T extends Number> Gatherer<T, ?, Double> runningAverage() {
        class State {

            private double sum;
            private long count;
        }

        Supplier<State> initializer = State::new;
        Integrator.Greedy<State, T, Double> integrator
                = (state, element, downstream) -> {
                    double doubleElement = element.doubleValue();
                    state.sum += doubleElement;
                    state.count++;
                    downstream.push(state.sum / state.count);

                    return true;
                };

        return Gatherer.ofSequential(initializer, integrator);

    }

    public static <T> Gatherer<T, ?, List<T>> chunks(int size) {
        Supplier<List<T>> initializer = ArrayList<T>::new;
        Integrator.Greedy<List<T>, T, List<T>> integrator
                = (state, element, downstream) -> {
                    state.add(element);
                    if (state.size() == size) {
                        downstream.push(List.copyOf(state));
                        state.clear();
                    }

                    return true;
                };
        BiConsumer<List<T>, Downstream<? super List<T>>> finisher
                = (state, downstream) -> {
                    downstream.push(List.copyOf(state));
                };

        return Gatherer.ofSequential(initializer, integrator, finisher);
    }

    public static <T> Gatherer<T, ?, List<T>> slidingWindow(int size) {
        class State {

            SequencedCollection<T> queue;
            boolean emitted;

            public State() {
                queue = new ArrayDeque<>(size);
                emitted = false;
            }

            boolean integrate(T element, Downstream<? super List<T>> downstream) {
                queue.addLast(element);
                if (queue.size() == size) {
                    emitted = true;
                    downstream.push(List.copyOf(queue));
                    queue.removeFirst();
                }

                return true;
            }

            void finish(Downstream<? super List<T>> downstream) {
                if (!emitted) {
                    downstream.push(List.copyOf(queue));
                }
            }
        }

        return Gatherer.<T, State, List<T>>ofSequential(
                State::new,
                Integrator.<State, T, List<T>>ofGreedy(State::integrate),
                State::finish
        );
    }

    public static <T> Gatherer<T, ?, T> sorted(Comparator<? super T> comparator) {
        Supplier<List<T>> initializer = ArrayList::new;
        Integrator.Greedy<List<T>, T, T> integrator = (state, element, _) -> {
            state.addLast(element);
            return true;
        };
        BiConsumer<List<T>, Downstream<? super T>> finisher
                = (state, downstream) -> {
                    state.sort(comparator);
                    state.forEach(downstream::push);
                };

        return Gatherer.ofSequential(initializer, integrator, finisher);
    }

    public static <T> Gatherer<T, ?, List<T>> increasingSequence(Comparator<? super T> comparator) {
        Supplier<List<T>> initializer = ArrayList::new;
        Integrator.Greedy<List<T>, T, List<T>> integrator
                = (state, element, downstream) -> {
                    boolean increasing = state.isEmpty() || comparator.compare(state.getLast(), element) < 0;

                    if (!increasing) {
                        downstream.push(List.copyOf(state));
                        state.clear();
                    }
                    state.add(element);
                    return true;
                };
        BiConsumer<List<T>, Downstream<? super List<T>>> finisher = (state, downstream) -> {
            downstream.push(List.copyOf(state));
        };

        return Gatherer.ofSequential(initializer, integrator, finisher);
    }
}
