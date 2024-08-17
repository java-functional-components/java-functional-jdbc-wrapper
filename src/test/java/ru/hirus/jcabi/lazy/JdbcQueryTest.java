package ru.hirus.jcabi.lazy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static ru.hirus.jcabi.misc.TestRecordQueries.createTable;
import static ru.hirus.jcabi.misc.TestRecordQueries.insert;
import static ru.hirus.jcabi.misc.TestRecordQueries.select;

import com.jcabi.jdbc.JdbcSession;
import org.junit.jupiter.api.Test;
import ru.hirus.jcabi.Nothing;
import ru.hirus.jcabi.misc.PostgresDatabaseTestTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcQueryTest extends PostgresDatabaseTestTemplate {

    @Test
    void queryTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        Nothing executeResult = createTable().execute(dataSource);
        assertSame(Nothing.INSTANCE, executeResult);

        // --- First connection with rollback
        JdbcSession jdbcSession = staticJdbcSession().autocommit(false);

        String insertedName = "MyName";
        long id = insert(insertedName).prepare(jdbcSession).execute();
        assertEquals(1, id);

        String name = select(id).prepare(jdbcSession).execute();
        assertEquals(insertedName, name);

        jdbcSession.rollback();

        // --- Second connection without rollback
        name = select(id).execute(dataSource);
        assertNull(name);

        id = insert(insertedName).execute(dataSource);
        assertEquals(2, id);

        name = select(id).execute(dataSource);
        assertEquals(insertedName, name);

        // --- Third connection to verify data
        name = select(id).execute(dataSource);
        assertEquals(insertedName, name);

        name = select(id + 1).execute(dataSource);
        assertNull(name);
    }

    @Test
    void flatMapTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Flat map call

        String insertedName = "Name";
        long result = insert(insertedName).map(i -> i + 1).execute(dataSource);
        assertEquals(2, result);

        int length = select(1).map(String::length).execute(dataSource);
        assertEquals(insertedName.length(), length);
    }

    @Test
    void mapComposeTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Compose call
        String insertedName = "MyName";
        String name = insert(insertedName)
                .thenCompose(i -> select(i))
                .execute(dataSource);

        assertEquals(name, insertedName);
    }

    @Test
    void composeTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Compose call
        String insertedName = "MyName";
        String name = insert(insertedName)
                .then(select(1))
                .execute(dataSource);

        assertEquals(insertedName, name);
    }
}
