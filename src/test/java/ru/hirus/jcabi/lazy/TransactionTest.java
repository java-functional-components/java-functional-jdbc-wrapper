package ru.hirus.jcabi.lazy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.hirus.jcabi.misc.TestRecordQueries.createTable;
import static ru.hirus.jcabi.misc.TestRecordQueries.insert;
import static ru.hirus.jcabi.misc.TestRecordQueries.select;
import static ru.hirus.jcabi.misc.TestRecordTransactions.insertSelect;

import com.jcabi.jdbc.JdbcSession;
import org.junit.jupiter.api.Test;
import ru.hirus.jcabi.misc.PostgresDatabaseTestTemplate;
import ru.hirus.jcabi.misc.TestRecord;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

public class TransactionTest extends PostgresDatabaseTestTemplate {

    @Test
    void transactionSimpleTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- First connection with rollback
        JdbcSession jdbcSession = staticJdbcSession().autocommit(false);

        String insertedName = "Name1";
        TestRecord testRecord1 = insertSelect(insertedName).prepare(jdbcSession).execute();
        assertEquals(new TestRecord(1, insertedName), testRecord1);

        insertedName = insertedName.repeat(2);
        TestRecord testRecord2 = insertSelect(insertedName).prepare(jdbcSession).execute();
        assertEquals(new TestRecord(2, insertedName), testRecord2);

        jdbcSession.rollback();

        // --- Second connection without rollback
        insertedName = "Name2";
        testRecord1 = insertSelect(insertedName).execute(dataSource);
        assertEquals(new TestRecord(3, insertedName), testRecord1);

        insertedName = insertedName.repeat(2);
        testRecord2 = insertSelect(insertedName).execute(dataSource);
        assertEquals(new TestRecord(4, insertedName), testRecord2);

        // --- Third connection to verify data
        assertEquals(insertedName.substring(0, insertedName.length() / 2), select(testRecord1.id()).execute(dataSource));
        assertEquals(insertedName, select(testRecord2.id()).execute(dataSource));
        assertNull(select(testRecord2.id() + 1).execute(dataSource));
    }

    @Test
    void transactionComplexTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Simulate three parallel transactions
        JdbcSession jdbcSession1 = staticJdbcSession().autocommit(false);
        JdbcSession jdbcSession2 = staticJdbcSession().autocommit(false);
        JdbcSession jdbcSession3 = staticJdbcSession().autocommit(false);

        TestRecord testRecord1 = insertSelect("Name1").prepare(jdbcSession1).execute();
        TestRecord testRecord2 = insertSelect("Name2").prepare(jdbcSession2).execute();
        TestRecord testRecord3 = insertSelect("Name3").prepare(jdbcSession3).execute();

        assertEquals(new TestRecord(1, "Name1"), testRecord1);
        assertEquals(new TestRecord(2, "Name2"), testRecord2);
        assertEquals(new TestRecord(3, "Name3"), testRecord3);

        assertNull(select(2).prepare(jdbcSession1).execute());
        assertNull(select(3).prepare(jdbcSession1).execute());

        assertNull(select(1).prepare(jdbcSession2).execute());
        assertNull(select(3).prepare(jdbcSession2).execute());

        assertNull(select(1).prepare(jdbcSession3).execute());
        assertNull(select(2).prepare(jdbcSession3).execute());

        jdbcSession1.commit();

        assertEquals("Name1", select(1).prepare(jdbcSession2).execute());
        assertEquals("Name1", select(1).prepare(jdbcSession3).execute());

        jdbcSession2.commit();

        assertEquals("Name2", select(2).prepare(jdbcSession3).execute());
    }

    @Test
    void flatMapTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Flat map call
        String insertedName = "Name";
        List<TestRecord> list = insertSelect(insertedName).map(List::of).execute(dataSource);

        assertEquals(List.of(new TestRecord(1, insertedName)), list);

        int length = new Transaction<>(select(1)).map(String::length).execute(dataSource);
        assertEquals(insertedName.length(), length);
    }

    @Test
    void mapComposeTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Compose call
        String insertedName = "MyName";
        String name = new Transaction<>(insert(insertedName))
                .thenCompose(i -> new Transaction<>(insert(insertedName + i)))
                .thenCompose(i -> new Transaction<>(select(i)))
                .execute(dataSource);

        assertEquals(insertedName + 1, name);
    }

    @Test
    void composeTest() throws SQLException {
        // --- Create table
        DataSource dataSource = factoryDataSource();

        createTable().execute(dataSource);

        // --- Compose call
        JdbcSession jdbcSession = staticJdbcSession().autocommit(false);

        String insertedName = "MyName";
        String name = new Transaction<>(insert(insertedName))
                .then(new Transaction<>(insert(insertedName + 1)))
                .then(new Transaction<>(select(2)))
                .execute(dataSource);

        assertEquals(name, insertedName + 1);
    }
}
