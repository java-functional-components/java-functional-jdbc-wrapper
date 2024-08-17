package ru.hirus.jcabi.lazy;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;
import com.jcabi.jdbc.Preparation;
import org.intellij.lang.annotations.Language;
import ru.hirus.jcabi.Nothing;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

/**
 * Represents the execution of JDBC query
 * <br>
 * <b>This class is immutable</b>
 *
 * @param <R> result type
 * @author LeonidM
 */
public final class JdbcQuery<R> implements Query<R> {

    private final Exception exception;
    private final List<Preparation> preparations;
    private final List<Object> arguments;
    private final OptionalJdbcExecutor executor;
    private final String sql;
    private final Optional<Outcome<R>> outcome;

    private JdbcQuery(List<Preparation> preparations, List<Object> arguments, OptionalJdbcExecutor executor,
                      String sql, Optional<Outcome<R>> outcome) {
        this.exception = new Exception("Stack trace of initialization");

        this.preparations = new ArrayList<>(preparations);
        this.arguments = new ArrayList<>(arguments);
        this.executor = executor;

        this.sql = sql;
        this.outcome = outcome;
    }

    /**
     * @return new instance of {@link Builder} for
     * {@link JdbcSession#select(Outcome)} query
     */
    public static BuilderSql select() {
        return new BuilderSql(JdbcSession::select);
    }

    /**
     * @return new instance of {@link Builder} for
     * {@link JdbcSession#insert(Outcome)} query
     */
    public static BuilderSql insert() {
        return new BuilderSql(JdbcSession::insert);
    }

    /**
     * @return new instance of {@link Builder} for
     * {@link JdbcSession#update(Outcome)} query
     */
    public static BuilderSql update() {
        return new BuilderSql(JdbcSession::update);
    }

    /**
     * @return new instance of {@link Builder} for
     * {@link JdbcSession#call(Outcome)} query
     */
    public static BuilderSql call() {
        return new BuilderSql(JdbcSession::call);
    }

    /**
     * @return new instance of {@link Builder} for
     * {@link JdbcSession#execute()} query
     */
    public static BuilderSql execute() {
        return new BuilderSql(new OptionalJdbcExecutor() {
            @Override
            public <R> R execute(JdbcSession jdbcSession, Optional<Outcome<R>> argument) throws SQLException {
                jdbcSession.execute();
                return (R) Nothing.INSTANCE;
            }
        }, true);
    }

    @Override
    public <N> Transaction<N> thenCompose(Function<? super R, Query<? extends N>> sqlExecutable) {
        return new Transaction<>(this).thenCompose(sqlExecutable);
    }

    @Override
    public <N> Transaction<N> then(Query<? extends N> query) {
        return thenCompose(r -> query);
    }

    @Override
    public <N> Transaction<N> thenApply(Query<Function<? super R, ? extends N>> mapper) {
        return thenCompose(r -> (jdbcSession -> new SqlRunnable<>(() -> mapper.prepare(jdbcSession).execute().apply(r))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <N> JdbcQuery<N> map(Function<? super R, ? extends N> mapper) {
        return new JdbcQuery<>(preparations, arguments, executor, sql, outcome.map(outcome1 ->
                (resultSet, statement) -> {
                    R result = outcome1.handle(resultSet, statement);
                    return mapper.apply(result);
                }));
    }

    /**
     * Executes database query using <i>jcabi-jdbc</i>
     *
     * @return result of the query
     */
    @Override
    public SqlRunnable<R> prepare(JdbcSession jdbcSession) {
        return new SqlRunnable<>(() -> {
            try {
                jdbcSession.sql(sql);

                for (Object argument : arguments) {
                    jdbcSession.set(argument);
                }

                for (Preparation preparation : preparations) {
                    jdbcSession.prepare(preparation);
                }

                return executor.execute(jdbcSession, outcome);
            } catch (SQLException e) {
                e.addSuppressed(exception);
                throw e;
            }
        });
    }

    // Стоит ли выносить этот класс и один ниже в отдельные файлы?

    /**
     * Adapter for JdbcSession that is used to call the right method
     * at the end of {@link JdbcQuery#prepare(JdbcSession)} method
     */
    @FunctionalInterface
    public interface OptionalJdbcExecutor {
        /**
         * Calls {@link JdbcSession} method with provided
         * optional {@link Outcome} argument
         *
         * @param <R> result type
         * @return result of the query
         */
        <R> R execute(JdbcSession jdbcSession, Optional<Outcome<R>> argument) throws SQLException;
    }

    // Не знаю, насколько это действительно удобнее другим, т.к. я понимаю, что
    // это может усложнить понимание, что и как тут происходит.
    // Я люблю делать такие недоврапперы, конечно, но могу и вырезать, если надо

    /**
     * Adapter for JdbcSession that is used to call the right method
     * at the end of {@link JdbcQuery#prepare(JdbcSession)} method.
     * <br>
     * The key difference between {@link JdbcExecutor} and {@link OptionalJdbcExecutor}
     * is that the first one always requires {@link Outcome} and the second one
     * does not
     */
    @FunctionalInterface
    public interface JdbcExecutor {
        /**
         * Calls {@link JdbcSession} method with provided
         * {@link Outcome} argument
         *
         * @param <R> result type
         * @return result of the query
         */
        <R> R execute(JdbcSession jdbcSession, Outcome<R> argument) throws SQLException;

        /**
         * Wraps {@link JdbcExecutor} to {@link OptionalJdbcExecutor}
         */
        default OptionalJdbcExecutor wrap() {
            return new OptionalJdbcExecutor() {
                @Override
                public <R> R execute(JdbcSession jdbcSession, Optional<Outcome<R>> argument) throws SQLException {
                    return JdbcExecutor.this.execute(jdbcSession, argument.orElseThrow());
                }
            };
        }
    }

    public static final class BuilderSql {

        private final OptionalJdbcExecutor executor;
        private final boolean emptyOutcome;

        /**
         * @param executor     adapter that calls corresponding {@link JdbcSession} method
         * @param emptyOutcome if true, outcome must be empty, otherwise not
         */
        public BuilderSql(OptionalJdbcExecutor executor, boolean emptyOutcome) {
            this.executor = executor;
            this.emptyOutcome = emptyOutcome;
        }

        /**
         * @param executor adapter that calls corresponding {@link JdbcSession} method
         */
        public BuilderSql(JdbcExecutor executor) {
            this(executor.wrap(), false);
        }

        /**
         * @see JdbcSession#sql(String)
         */
        public Builder sql(@Language("PostgreSQL") String sql) {
            return new Builder(executor, emptyOutcome, sql);
        }
    }

    public static final class Builder {
        private final List<Preparation> preparations;
        private final List<Object> arguments;
        private final OptionalJdbcExecutor executor;
        private final boolean emptyOutcome;
        private final String sql;

        /**
         * @param executor     adapter that calls corresponding {@link JdbcSession} method
         * @param emptyOutcome if true, outcome must be empty, otherwise not
         * @param sql          SQL query
         */
        public Builder(OptionalJdbcExecutor executor, boolean emptyOutcome, String sql) {
            preparations = new LinkedList<>();
            arguments = new LinkedList<>();

            this.executor = executor;
            this.emptyOutcome = emptyOutcome;

            this.sql = sql;
        }

        /**
         * @param executor adapter that calls corresponding {@link JdbcSession} method
         * @param sql      SQL query
         */
        public Builder(JdbcExecutor executor, String sql) {
            this(executor.wrap(), false, sql);
        }

        /**
         * @see JdbcSession#set(Object)
         */
        public Builder set(Object... arguments) {
            if (Objects.isNull(arguments)) {
                this.arguments.add(null);
                return this;
            }
            this.arguments.addAll(Arrays.asList(arguments));
            return this;
        }

        public Builder setArray(Object[] array) {
            this.arguments.add(array);
            return this;
        }

        /**
         * @see JdbcSession#prepare(Preparation)
         */
        public Builder prepare(Preparation preparation) {
            preparations.add(preparation);
            return this;
        }

        /**
         * Builds {@link JdbcQuery}
         *
         * @param outcome outcome that will be used to get the result
         * @param <R>     result type
         * @throws NotCompleteQueryException if SQL is missing or if
         *                                   {@link Builder#emptyOutcome} is true
         */
        public <R> JdbcQuery<R> build(Outcome<R> outcome) throws NotCompleteQueryException {
            return build(Optional.of(outcome));
        }

        /**
         * Builds {@link JdbcQuery} that does not return anything
         *
         * @throws NotCompleteQueryException if SQL is missing or if
         *                                   {@link Builder#emptyOutcome} is false
         */
        public JdbcQuery<Nothing> build() throws NotCompleteQueryException {
            return build(Optional.empty());
        }

        private <R> JdbcQuery<R> build(Optional<Outcome<R>> outcome) throws NotCompleteQueryException {
            validate(outcome);
            return new JdbcQuery<>(preparations, arguments, executor, sql, outcome);
        }

        private <R> void validate(Optional<Outcome<R>> outcome) throws NotCompleteQueryException {
            if (outcome.isEmpty() != emptyOutcome) {
                if (outcome.isEmpty()) {
                    throw new NotCompleteQueryException("outcome is empty");
                } else {
                    throw new NotCompleteQueryException("outcome must be empty");
                }
            }
        }

        // Честно говоря, я пока сам до конца не знаю, как будет правильнее поступить
        // с этой ошибкой. До этого я делал наследование от SQLException, но после этого
        // стали возникать проблемы с Stream#map, которых не было до рефакторинга, т.к.
        // валидация и, следовательно, пробрасывание этого исключения шло именно в момент
        // выполнения запроса. После того как я перешёл на билдеры, валидацию уже приходится
        // делать при вызове метода build, что, как раз-таки, всё портит :(
        //
        // На мой взгляд, это исключение обрабатывать не стоит, потому что оно появляется,
        // когда программист, использующий это API, ошибся на этапе билдера,
        // что не должно будет просочиться на прод из-за наличия тестов.

        /**
         * Exception, indicating that {@link Builder} was called incorrectly
         */
        public static class NotCompleteQueryException extends RuntimeException {
            public NotCompleteQueryException(String message) {
                super(message);
            }
        }
    }

}
