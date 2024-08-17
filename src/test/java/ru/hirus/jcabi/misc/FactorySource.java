package ru.hirus.jcabi.misc;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class FactorySource implements DataSource {

    private final ConnectionFactory connectionFactory;
    private Connection connection;

    public FactorySource(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = connectionFactory.create();
        }

        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public PrintWriter getLogWriter() {
        throw new UnsupportedOperationException("#getLogWriter()");
    }

    @Override
    public void setLogWriter(final PrintWriter writer) {
        throw new UnsupportedOperationException("#setLogWriter()");
    }

    @Override
    public void setLoginTimeout(final int seconds) {
        throw new UnsupportedOperationException("#setLoginTimeout()");
    }

    @Override
    public int getLoginTimeout() {
        throw new UnsupportedOperationException("#getLoginTimeout()");
    }

    @Override
    public Logger getParentLogger() {
        throw new UnsupportedOperationException("#getParentLogger()");
    }

    @Override
    public <T> T unwrap(final Class<T> iface) {
        throw new UnsupportedOperationException("#unwrap()");
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) {
        throw new UnsupportedOperationException("#isWrapperFor()");
    }

    @FunctionalInterface
    public interface ConnectionFactory {
        Connection create() throws SQLException;
    }
}
