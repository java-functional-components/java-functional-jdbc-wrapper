package ru.hirus.jcabi.lazy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jcabi.jdbc.UrlSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class SequenceTest {

    @Test
    void sequenceTest() throws SQLException {
        // --- Sequence call
        DataSource dataSource = new UrlSource("...");

        List<Long> longs = LongStream.rangeClosed(1, 10)
                .boxed()
                .collect(Collectors.toList());

        List<Query<Long>> queries = longs.stream()
                .map(Query::pure)
                .collect(Collectors.toList());

        Query<List<Long>> sequence = new Query.Sequence<>(queries);

        List<Long> sequenceLongs = sequence.execute(dataSource);
        assertEquals(longs, sequenceLongs);
    }
}
