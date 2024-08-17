package ru.hirus.jcabi.lazy.forcomprehension;

import com.jcabi.jdbc.JdbcSession;
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import ru.hirus.jcabi.lazy.Query;
import ru.hirus.jcabi.lazy.SqlRunnable;
import ru.hirus.libextension.Function3;
import ru.hirus.libextension.Function4;

import java.util.function.BiFunction;
import java.util.function.Function;

public record Do5<A, B, C, D, E>(
        Query<A> expr1,
        Function<A, Query<B>> expr2,
        BiFunction<A, B, Query<C>> expr3,
        Function3<A, B, C, Query<D>> expr4,
        Function4<A, B, C, D, Query<E>> expr5
) implements Query<Quintet<A, B, C, D, E>> {

    @Override
    public SqlRunnable<Quintet<A, B, C, D, E>> prepare(JdbcSession jdbcSession) {
        return new Do4<>(expr1, expr2, expr3, expr4)
                .thenCompose(quartet ->
                        expr5.apply(quartet.getValue0(), quartet.getValue1(), quartet.getValue2(), quartet.getValue3())
                                .map(c ->
                                        Quintet.with(
                                                quartet.getValue0(),
                                                quartet.getValue1(),
                                                quartet.getValue2(),
                                                quartet.getValue3(),
                                                c)))
                .prepare(jdbcSession);
    }
}
