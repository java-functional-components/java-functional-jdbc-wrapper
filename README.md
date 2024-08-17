# lazy-jcabi-jdbc

Этот модуль добавляет транзакционный интерфейс-функтор `SqlExecutable` для решения следующих задач:
* Корректная обработка вложенных транзакций
* Ленивость запросов и их инициализации
* Потокобезопасность

# Пример использования

## 1. Инициализация Query
```java
import com.jcabi.jdbc.SingleOutcome;
import ru.hirus.jcabi.Nothing;
import ru.hirus.jcabi.lazy.Query;

public class Repository {

    public Query<Nothing> createTable() {
        return Query.execute() // JdbcQuery.Builder
                .sql("""
                        CREATE TABLE IF NOT EXISTS test (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(128)
                        )
                        """)
                .build(); // JdbcQuery<Nothing>
    }

    public Query<Long> insert(String name) {
        return Query.insert() // JdbcQuery.Builder
                .sql("INSERT INTO test (name) VALUES (?) RETURNING id")
                .set(name)
                .build(new SingleOutcome<>(Long.class)); // JdbcQuery<Long>
    }

    public Query<Record> select(long id) {
        return Query.select() // JdbcQuery.Builder
                .sql("SELECT name FROM test WHERE id = ?")
                .set(id)
                .build(new SingleOutcome<>(String.class)) // JdbcQuery<String>
                .map(name -> new Record(id, name)); // JdbcQuery<Record>
    }

    public record Record(long id, String name) {
    }
}
```

## 2. Работа с Query
```java
import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.UrlSource;
import ru.hirus.jcabi.Nothing;
import ru.hirus.jcabi.lazy.Query;

import java.sql.SQLException;

public final class QueryMain {

    public static void main(String[] args) throws SQLException {
        Repository repository = new Repository();

        DataSource dataSource = new UrlSource("...");

        // Инициализация таблицы
        Query<Nothing> createTableQuery = repository.createTable();
        createTableQuery.execute(dataSource);
        
        // Не вызывайте метод ниже! Он используется исключительно
        // внутри метода SqlExecutable#execute(DataSource).
        // С объектом ниже у вас ничего не получится сделать
        Query<Nothing> runnable = createTableQuery.prepare();

        // Вставка имени
        Query<Long> insertQuery = repository.insert("Name");
        long id = insertQuery.execute(dataSource);

        // Получение записи
        Query<Repository.Record> selectQuery = repository.select(1);
        Repository.Record record = selectQuery.execute(dataSource);
    }
}
```

## 3. Работа с Transaction
```java
import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.UrlSource;
import ru.hirus.jcabi.Nothing;
import ru.hirus.jcabi.lazy.Query;
import ru.hirus.jcabi.lazy.Transaction;

import java.sql.SQLException;

public final class TransactionMain {

    public static void main(String[] args) throws SQLException {
        Repository repository = new Repository();

        DataSource dataSource = new UrlSource("...")
                .autocommit(false);

        Query<Nothing> createTableQuery = repository.createTable();
        Query<Long> insertQuery = repository.insert("Name");

        // then делает композицию двух SqlExecutable
        // (Query и Transaction ими и являются), собирая
        // их в транзакцию
        Transaction<Long> transaction1 = createTableQuery
                .then(insertQuery);

        // thenCompose также делает композицию, но в этом
        // случае учитывается результат предыдущей операции
        // транзакции и прокидывается в функцию
        Transaction<Repository.Record> transaction2 = transaction1
                .thenCompose(id -> repository.select(id));

        // Внутри метода SqlExecutable#execute(DataSource)
        // будет создан объект JdbcSession, который будет
        // передан уже в сами запросы
        Repository.Record record = transaction2.execute(dataSource);
    }
}
```

## 4. Утилиты
### 4.1. Sequence
```java
import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.UrlSource;
import ru.hirus.jcabi.lazy.Query;

import java.sql.SQLException;
import java.util.List;

public final class SequenceMain {

    public static void main(String[] args) throws SQLException {
        Repository repository = new Repository();

        DataSource dataSource = new UrlSource("...");

        // Конвертация List<SqlExecutable<R>> в SqlExecutable<List<R>>
        List<Query<Repository.Record>> queries = List.of(
                repository.select(1),
                repository.select(2),
                repository.select(3)
        );

        Query<List<Repository.Record>> sequence = new Query.Sequence<>(queries);
        List<Repository.Record> records = sequence.execute(dataSource);
    }
}
```
