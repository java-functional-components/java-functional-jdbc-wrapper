package ru.hirus.jcabi.lazy;

import com.jcabi.jdbc.JdbcSession;
import ru.hirus.jcabi.Nothing;

import java.util.Optional;
import java.util.function.Function;

/**
 * Represents the transaction of one and more {@link Query}
 * <br>
 * <b>This class is immutable</b>
 *
 * @param <R> result type
 * @author LeonidM
 */
public final class Transaction<R> implements Query<R> {

    private final Function<Object, Query<? extends R>> sqlExecutable;
    private final Optional<Transaction<?>> previous;

    private <P> Transaction(Function<? super P, Query<? extends R>> sqlExecutable, Transaction<P> previous) {
        this.sqlExecutable = (Function<Object, Query<? extends R>>) sqlExecutable;
        this.previous = Optional.of(previous);
    }

    public Transaction(Query<? extends R> query) {
        this.sqlExecutable = r -> query;
        this.previous = Optional.empty();
    }

    /**
     * Composes this {@link Transaction} with {@link Query} that
     * will be initialized using the result of this {@link Transaction}
     * <br>
     * <b>This method is lazy</b>
     *
     * @param sqlExecutable function that takes results of this {@link Transaction}
     *                      and initializes next {@link Query}
     * @param <N>           new result type
     * @return new transaction, containing this transaction and provided executable
     */
    @Override
    public <N> Transaction<N> thenCompose(Function<? super R, Query<? extends N>> sqlExecutable) {
        return new Transaction<>(sqlExecutable, this);
    }

    /**
     * Composes this {@link Transaction} with already initialized
     * provided {@link Query}
     *
     * @param <N> new result type
     * @return new transaction, containing this transaction and provided executable
     */
    @Override
    public <N> Transaction<N> then(Query<? extends N> query) {
        return thenCompose(r -> query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N> Transaction<N> map(Function<? super R, ? extends N> mapper) {
        // Стоит ли это вынести в приватный метод?
        Function<Object, Query<? extends N>> fmap = p -> {
            Query<? extends R> apply = sqlExecutable.apply(p);
            return apply.<N>map(mapper::apply);
        };

        if (previous.isEmpty()) {
            return new Transaction<>(fmap.apply(Nothing.INSTANCE));
        } else {
            return new Transaction<>(fmap::apply, previous.get());
        }
    }

    /**
     * Executes this and all previous executables wrapped
     *
     * @return result of the transaction
     */
    @Override
    public SqlRunnable<R> prepare(JdbcSession jdbcSession) {
        return new SqlRunnable<>(() -> {
            Query<? extends R> obtainedQuery;

            if (previous.isPresent()) {
                Object result = previous.get().prepare(jdbcSession).execute();
                obtainedQuery = sqlExecutable.apply(result);
            } else {
                obtainedQuery = sqlExecutable.apply(Nothing.INSTANCE);
            }

            return obtainedQuery.prepare(jdbcSession).execute();
        });
    }
}
