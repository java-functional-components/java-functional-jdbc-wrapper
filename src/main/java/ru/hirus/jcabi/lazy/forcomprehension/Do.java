package ru.hirus.jcabi.lazy.forcomprehension;

import io.vavr.Function3;
import io.vavr.Function4;
import ru.hirus.jcabi.lazy.Query;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Do {
    public static <A, B> Do2<A, B> of(Query<A> a, Function<A, Query<B>> fb) {
        return new Do2<>(a, fb);
    }

    public static <A, B, C> Do3<A, B, C> of(
            Query<A> a,
            Function<A, Query<B>> fb,
            BiFunction<A, B, Query<C>> fc) {
        return new Do3<>(a, fb, fc);
    }

    public static <A, B, C, D> Do4<A, B, C, D> of(
            Query<A> a,
            Function<A, Query<B>> fb,
            BiFunction<A, B, Query<C>> fc,
            Function3<A, B, C, Query<D>> fd) {
        return new Do4<>(a, fb, fc, fd);
    }

    public static <A, B, C, D, E> Do5<A, B, C, D, E> of(
            Query<A> a,
            Function<A, Query<B>> fb,
            BiFunction<A, B, Query<C>> fc,
            Function3<A, B, C, Query<D>> fd,
            Function4<A, B, C, D, Query<E>> fe) {
        return new Do5<>(a, fb, fc, fd, fe);
    }
}
