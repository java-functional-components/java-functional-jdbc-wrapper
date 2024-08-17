package ru.hirus.jcabi.misc;

import com.jcabi.jdbc.SingleOutcome;
import ru.hirus.jcabi.Nothing;
import ru.hirus.jcabi.lazy.Query;

public final class TestRecordQueries {

    private TestRecordQueries() {

    }

    public static Query<Nothing> createTable() {
        return Query.execute()
                .sql("""
                        CREATE TABLE test (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(128)
                        )
                        """)
                .build();
    }

    public static Query<Long> insert(String name) {
        return Query.insert()
                .sql("INSERT INTO test (name) VALUES (?) RETURNING id")
                .set(name)
                .build(new SingleOutcome<>(Long.class));
    }

    public static Query<String> select(long id) {
        return Query.select()
                .sql("SELECT name FROM test WHERE id = ?")
                .set(id)
                .build(new SingleOutcome<>(String.class, true));
    }

    public static Query<Nothing> dropTable() {
        return Query.execute()
                .sql("DROP TABLE IF EXISTS test")
                .build();
    }

}
