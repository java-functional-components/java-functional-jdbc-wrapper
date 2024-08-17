package ru.hirus.jcabi.lazy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jcabi.jdbc.UrlSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.function.Function;

public class QueryTest {

    private static final DataSource DATA_SOURCE = new UrlSource("...");

    @Test
    void flatMapTest() throws SQLException {
        String string = "MyName";

        Query<String> stringExecutable = Query.pure(string);
        Query<String> repeatExecutable = stringExecutable.map(s -> s.repeat(2));
        Query<Integer> lengthExecutable = repeatExecutable.map(String::length);

        int length = lengthExecutable.execute(DATA_SOURCE);

        assertEquals(string.repeat(2).length(), length);
    }

    @Test
    void applicativeApplyTest() throws SQLException {
        String string = "MyName";

        Query<String> stringExecutable = Query.pure(string);
        Query<Function<? super String, ? extends Integer>> functionExecutable = Query.pure(s -> s.repeat(2).length());

        Query<Integer> transaction = stringExecutable.thenApply(functionExecutable);

        int length = transaction.execute(DATA_SOURCE);

        assertEquals(string.repeat(2).length(), length);
    }

    @Test
    void mapComposeTest() throws SQLException {
        String string = "MyName";

        Query<String> stringExecutable = Query.pure(string);
        Function<? super String, Query<? extends Integer>> function = s -> Query.pure(s.repeat(2).length());

        Query<Integer> executable = stringExecutable.thenCompose(function);

        int length = executable.execute(DATA_SOURCE);

        assertEquals(string.repeat(2).length(), length);
    }

    @Test
    void composeTest() throws SQLException {
        String string = "MyName";

        Query<String> stringExecutable = Query.pure(string);
        Query<Integer> function = Query.pure(string.repeat(2).length());

        Query<Integer> executable = stringExecutable.then(function);

        int length = executable.execute(DATA_SOURCE);

        assertEquals(string.repeat(2).length(), length);
    }

}
