package ru.hirus.jcabi.misc;

import static ru.hirus.jcabi.misc.TestRecordQueries.insert;
import static ru.hirus.jcabi.misc.TestRecordQueries.select;

import org.testcontainers.shaded.com.google.common.collect.Iterables;
import org.testcontainers.shaded.com.google.common.collect.Lists;
import ru.hirus.jcabi.lazy.Query;
import ru.hirus.jcabi.lazy.Transaction;

import java.util.List;

public final class TestRecordTransactions {

    private TestRecordTransactions() {

    }

    public static Query<TestRecord> insertSelect(String name) {
        return insert(name)
                .thenCompose(id -> select(id).map(n -> new TestRecord(id, n)));
    }

    public static Transaction<List<TestRecord>> insertSelects(String firstName, String... names) {
        Transaction<List<TestRecord>> transaction = null;

        for (String name : Iterables.concat(List.of(firstName), List.of(names))) {
            if (transaction == null) {
                transaction = new Transaction<>(insertSelect(name).map(Lists::newArrayList));
            } else {
                transaction = transaction.thenCompose(list -> {
                    return insertSelect(name).map(testRecord -> {
                        list.add(testRecord);
                        return list;
                    });
                });
            }
        }

        return transaction;
    }
}
