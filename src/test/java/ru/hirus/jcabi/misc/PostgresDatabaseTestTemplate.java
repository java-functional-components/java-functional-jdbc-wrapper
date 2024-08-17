package ru.hirus.jcabi.misc;

import static ru.hirus.jcabi.misc.TestRecordQueries.dropTable;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.StaticSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class PostgresDatabaseTestTemplate {
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:16-alpine"
    );

    private List<DataSource> dataSources = new ArrayList<>();

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }


    @AfterEach
    void afterEach() throws SQLException {
        dropTable().execute(factoryDataSource());
    }

    @AfterEach
    void finish() throws SQLException {
        for (DataSource dataSource : dataSources) {
            if (dataSource instanceof StaticSource source) {
                source.getConnection().close();
            } else if (dataSource instanceof FactorySource factorySource) {
                factorySource.close();
            } else {
                throw new IllegalStateException("Unknown source " + dataSource.getClass());
            }
        }
    }

    static Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    protected JdbcSession staticJdbcSession() throws SQLException {
        StaticSource source = new StaticSource(connection());
        dataSources.add(source);
        return new JdbcSession(source);
    }

    protected StaticSource staticDataSource() throws SQLException {
        StaticSource source = new StaticSource(connection());
        dataSources.add(source);
        return source;
    }

    protected FactorySource factoryDataSource() {
        FactorySource source = new FactorySource(PostgresDatabaseTestTemplate::connection);
        dataSources.add(source);
        return source;
    }
}
