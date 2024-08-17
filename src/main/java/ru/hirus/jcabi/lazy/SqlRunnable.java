package ru.hirus.jcabi.lazy;

import java.sql.SQLException;

/**
 * Stores executor-function
 * <br>
 * This class is only needed to hide
 * execution operations from developers
 *
 * @param <R> result type
 * @author LeonidM
 */
public final class SqlRunnable<R> {

    private final CheckedSupplier<R> resultSupplier;

    public SqlRunnable(CheckedSupplier<R> resultSupplier) {
        this.resultSupplier = resultSupplier;
    }

    /**
     * Executes operation(s) related to the database
     * <br>
     * This method is package-private, because there
     * was the goal to prevent invalid executions
     * where, for example, a developer provides session
     * with enabled autocommit to the transaction, where
     * it must be disabled
     *
     * @return result of the operation(s)
     */
    R execute() throws SQLException {
        return resultSupplier.get();
    }

    public interface CheckedSupplier<R> {
        R get() throws SQLException;
    }
}
