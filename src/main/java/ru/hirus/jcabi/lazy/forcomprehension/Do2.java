package ru.hirus.jcabi.lazy.forcomprehension;

import com.jcabi.jdbc.JdbcSession;
import org.javatuples.Pair;
import ru.hirus.jcabi.lazy.Query;
import ru.hirus.jcabi.lazy.SqlRunnable;

import java.util.function.Function;

public record Do2<A, B>(
        Query<A> expr1,
        Function<A, Query<B>> expr2
) implements Query<Pair<A, B>>{

    @Override
    public SqlRunnable<Pair<A, B>> prepare(JdbcSession jdbcSession) {
        return expr1.thenCompose(a -> expr2.apply(a).map(b -> Pair.with(a, b))).prepare(jdbcSession);
    }
}
