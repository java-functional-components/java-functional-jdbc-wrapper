package ru.hirus.jcabi;

import ru.hirus.jcabi.lazy.JdbcQuery;

/**
 * This class is used to avoid null values in specific queries
 * like {@link JdbcQuery#execute()}
 *
 * @author LeonidM
 */
public final class Nothing {

    public static Nothing INSTANCE = new Nothing();

    private Nothing() {

    }
}
