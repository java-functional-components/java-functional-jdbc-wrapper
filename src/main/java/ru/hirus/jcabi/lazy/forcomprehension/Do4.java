package ru.hirus.jcabi.lazy.forcomprehension;

import com.jcabi.jdbc.JdbcSession;
import io.vavr.Function3;
import org.javatuples.Quartet;
import ru.hirus.jcabi.lazy.Query;
import ru.hirus.jcabi.lazy.SqlRunnable;

import java.util.function.BiFunction;
import java.util.function.Function;

public record Do4<A, B, C, D>(
        Query<A> expr1,
        Function<A, Query<B>> expr2,
        BiFunction<A, B, Query<C>> expr3,
        Function3<A, B, C, Query<D>> expr4
) implements Query<Quartet<A, B, C, D>> {

    @Override
    public SqlRunnable<Quartet<A, B, C, D>> prepare(JdbcSession jdbcSession) {
        return new Do3<>(expr1, expr2, expr3)
                .thenCompose(triplet ->
                        expr4.apply(triplet.getValue0(), triplet.getValue1(), triplet.getValue2())
                                .map(c ->
                                        Quartet.with(
                                                triplet.getValue0(),
                                                triplet.getValue1(),
                                                triplet.getValue2(),
                                                c)))
                .prepare(jdbcSession);
    }
}
