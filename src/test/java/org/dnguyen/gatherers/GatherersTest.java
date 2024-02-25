package org.dnguyen.gatherers;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class GatherersTest {

    @Test
    void addition() {
        assertEquals(2, 2);
    }

    @Test
    void doNothing() {
        var a = List.of(1, 2, 3);
        assertEquals(List.of(1, 2, 3), a.stream().gather(MyGatherers.doNothing()).toList());
    }

    @Test
    void inspect() {
        var a = List.of(1, 2, 3);
        assertEquals(List.of(1, 2, 3),
                a.stream().gather(MyGatherers.inspect(x -> System.out.println(x))).toList());
    }

    @Test
    void map() {
        var a = List.of("A", "B", "C", "D");
        assertEquals(List.of("AA", "BB", "CC", "DD"),
                a.stream()
                        .gather(MyGatherers.map(x -> x + x))
                        .toList()
        );

        assertEquals(List.of("a", "b", "c", "d"),
                a.stream()
                        .gather(MyGatherers.map(x -> x.toLowerCase()))
                        .toList());

    }

    @Test
    void filter() {
        assertEquals(List.of(1, 3, 5, 7),
                IntStream.range(1, 8)
                        .mapToObj(Integer::valueOf)
                        .gather(MyGatherers.filter(x -> x % 2 == 1))
                        .toList()
        );
    }

    @Test
    void flatMapIf() {
        assertEquals(List.of(1, 1, 2, 3, 3, 4, 5, 5, 6, 7, 7, 8),
                IntStream.range(1, 9)
                        .mapToObj(Integer::valueOf)
                        .gather(MyGatherers.flatMapIf(
                                x -> x % 2 == 1,
                                x -> Stream.of(x, x)
                        )).toList()
        );
    }

    @Test
    void takeWhileIncluding() {
        var a = List.of(1, 3, 5, 7, 2, 4, 6);
        assertEquals(
                List.of(1, 3, 5, 7, 2),
                a.stream()
                        .gather(MyGatherers.takeWhileIncluding(x -> x % 2 == 1))
                        .toList()
        );
    }

    @Test
    void limit() {
        assertEquals(List.of(1, 2, 3, 4),
                IntStream.range(1, 20)
                        .mapToObj(Integer::valueOf)
                        .gather(MyGatherers.limit(4))
                        .toList()
        );
    }

    @Test
    void increasing() {
        var a = List.of(1, 2, 3, 4, 3, 2, 1, 5, 6, 7);
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7),
                a.stream()
                        .gather(MyGatherers.increasing(Comparator.naturalOrder()))
                        .toList()
        );

    }

    @Test
    void runningAverage() {
        var a = List.of(1, 2, 3, 4, 3, 2);
        assertEquals(List.of(1.0, 1.5, 2.0, 2.5, 2.6, 2.5),
                a.stream()
                        .gather(MyGatherers.runningAverage())
                        .toList()
        );

    }

    @Test
    void chunks() {
        var a = List.of(1, 2, 3, 4, 3, 2, 1);
        assertEquals(List.of(List.of(1, 2, 3), List.of(4, 3, 2), List.of(1)),
                a.stream()
                        .gather(MyGatherers.chunks(3))
                        .toList()
        );
    }

    @Test
    void slidingWindow() {
        var a = List.of(1, 2, 3, 4, 3, 2, 1);
        assertEquals(List.of(List.of(1, 2, 3), List.of(2, 3, 4),
                List.of(3, 4, 3), List.of(4, 3, 2), List.of(3, 2, 1)),
                a.stream()
                        .gather(MyGatherers.slidingWindow(3))
                        .toList()
        );

        var b = List.of(1, 2);
        assertEquals(List.of(List.of(1, 2)),
                b.stream()
                        .gather(MyGatherers.slidingWindow(3))
                        .toList()
        );
    }

    @Test
    void sorted() {
        var a = List.of(1, 2, 3, 4, 3, 2, 1);
        assertEquals(List.of(1, 1, 2, 2, 3, 3, 4),
                a.stream()
                        .gather(MyGatherers.sorted(Comparator.naturalOrder()))
                        .toList()
        );
    }

    @Test
    void increasingSequence() {
        var a = List.of(1, 2, 3, 4, 3, 2, 1, 5, 6, 7);
        assertEquals(List.of(List.of(1, 2, 3, 4), List.of(3), List.of(2), List.of(1, 5, 6, 7)),
                a.stream()
                        .gather(MyGatherers.increasingSequence(Comparator.naturalOrder()))
                        .toList()
        );
    }
}
