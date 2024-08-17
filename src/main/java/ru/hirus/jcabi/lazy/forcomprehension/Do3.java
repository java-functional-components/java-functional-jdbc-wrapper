package ru.hirus.jcabi.lazy.forcomprehension;

import com.jcabi.jdbc.JdbcSession;
import org.javatuples.Triplet;
import ru.hirus.jcabi.lazy.Query;
import ru.hirus.jcabi.lazy.SqlRunnable;

import java.util.function.BiFunction;
import java.util.function.Function;

public record Do3<A, B, C>(
        Query<A> expr1,
        Function<A, Query<B>> expr2,
        BiFunction<A, B, Query<C>> expr3
) implements Query<Triplet<A, B, C>> {
    @Override
    public SqlRunnable<Triplet<A, B, C>> prepare(JdbcSession jdbcSession) {
        return new Do2<>(expr1, expr2)
                .thenCompose(pair ->
                        expr3.apply(pair.getValue0(), pair.getValue1())
                                .map(c -> Triplet.with(pair.getValue0(), pair.getValue1(), c)))
                .prepare(jdbcSession);
    }
}
