package ru.hirus.jcabi.lazy;

import com.jcabi.jdbc.JdbcSession;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents the execution of operation(s) related
 * to the database
 * <br>
 * <b>This interface is a monad</b>
 *
 * @param <R> result type
 * @author LeonidM
 */
@FunctionalInterface
public interface Query<R> {

    /**
     * Analogue of the "pure" function a.k.a. "return" from Haskell
     * that wraps provided object in {@code SqlExecutable<R>}
     *
     * @param <R> result type
     */
    static <R> Query<R> pure(R result) {
        return jdbcSession -> new SqlRunnable<>(() -> result);
    }

    /**
     * Wraps provided exception in {@code SqlExecutable<R>} that
     * will be thrown on {@link Query#execute(DataSource)}
     *
     * @param <R> result type
     */
    static <R> Query<R> exception(Supplier<SQLException> exception) {
        return jdbcSession -> new SqlRunnable<>(() -> {
            throw exception.get();
        });
    }

    /**
     * @see JdbcQuery#select()
     */
    static JdbcQuery.BuilderSql select() {
        return JdbcQuery.select();
    }

    /**
     * @see JdbcQuery#insert()
     */
    static JdbcQuery.BuilderSql insert() {
        return JdbcQuery.insert();
    }

    /**
     * @see JdbcQuery#update()
     */
    static JdbcQuery.BuilderSql update() {
        return JdbcQuery.update();
    }

    /**
     * @see JdbcQuery#call()
     */
    static JdbcQuery.BuilderSql call() {
        return JdbcQuery.call();
    }

    /**
     * @see JdbcQuery#execute()
     */
    static JdbcQuery.BuilderSql execute() {
        return JdbcQuery.execute();
    }

    /**
     * Prepares lazy supplier that executes operation(s)
     * related to the database
     * <br>
     * It does not return result itself, because
     * there was the goal to force usage of
     * {@link Query#execute(DataSource)} method
     * to prevent invalid executions where, for example,
     * a developer provides session with enabled autocommit
     * to the transaction, where it must be disabled
     *
     * @return result of the operation(s)
     */
    SqlRunnable<R> prepare(JdbcSession jdbcSession);

    /**
     * Executes operation(s) related to the database
     *
     * @return result of the operation(s)
     */
    default R execute(DataSource dataSource) throws SQLException {
        JdbcSession jdbcSession = new JdbcSession(dataSource).autocommit(false);
        R result = prepare(jdbcSession).execute();
        try {
            jdbcSession.commit();
        } catch (IllegalStateException e) {
            if (!e.getMessage().equals("Connection is not open, can't commit")) {
                throw new SQLException(e);
            }
        }

        return result;
    }

    /**
     * Analogue of the "{@code >>=}" operator from Haskell
     * that composes this {@link Query} with another one that
     * will be initialized using the result of this {@link Query}
     * <br>
     * <b>This method is lazy</b>
     *
     * @param sqlExecutable function that takes results of this {@link Query}
     *                      and initializes next one {@link Query}
     * @param <N>           new result type
     * @return new transaction, containing two executables
     */
    default <N> Transaction<N> thenCompose(Function<? super R, Query<? extends N>> sqlExecutable) {
        return new Transaction<>(
                (JdbcSession jdbcSession) -> new SqlRunnable<>(() -> {
                    R result = prepare(jdbcSession).execute();
                    return sqlExecutable.apply(result).prepare(jdbcSession).execute();
                }));
    }

    /**
     * Analogue of the "{@code >>}" operator from Haskell
     * that composes this {@link Query} with already initialized
     * another one
     * <br>
     * <b>This method is lazy</b>
     *
     * @param <N> new result type
     * @return new transaction, containing two executables
     */
    default <N> Query<N> then(Query<? extends N> query) {
        return thenCompose(r -> query);
    }

    /**
     * Analogue of the "{@code <*>}" operator from Haskell
     * that applies wrapped function {@code Function<R, N>}
     * to this {@code SqlExecutable<R>}
     * <br>
     * <b>This method is lazy</b>
     *
     * @param <N> new result type
     */
    default <N> Query<N> thenApply(Query<Function<? super R, ? extends N>> mapper) {
        return thenCompose(r -> (jdbcSession -> new SqlRunnable<>(() -> {
            Function<? super R, ? extends N> result = mapper.prepare(jdbcSession).execute();
            return result.apply(r);
        })));
    }

    /**
     * Analogue of the "fmap" function a.k.a. flat map from Haskell
     * that converts {@code SqlExecutable<R>} to {@code SqlExecutable<N>}
     * using provided mapper
     * <br>
     * <b>This method is lazy</b>
     *
     * @param <N> new result type
     */
    default <N> Query<N> map(Function<? super R, ? extends N> mapper) {
        return jdbcSession -> new SqlRunnable<>(() -> {
            R result = prepare(jdbcSession).execute();
            return mapper.apply(result);
        });
    }

    /**
     * Analogue of the "sequence" function from Haskell that converts
     * {@code List<SqlExecutable<R>>} to {@code SqlExecutable<List<R>>}
     *
     * @param <R> type of result elements in the list
     */
    class Sequence<R> implements Query<List<R>> {

        private final List<? extends Query<? extends R>> sqlExecutables;

        public Sequence(List<? extends Query<? extends R>> sqlExecutables) {
            this.sqlExecutables = sqlExecutables;
        }

        @Override
        public SqlRunnable<List<R>> prepare(JdbcSession jdbcSession) {
            return new SqlRunnable<>(() -> {
                List<R> list = new ArrayList<>();
                for (Query<? extends R> query : sqlExecutables) {
                    R result = query.prepare(jdbcSession).execute();
                    list.add(result);
                }
                return list;
            });
        }
    }

}
