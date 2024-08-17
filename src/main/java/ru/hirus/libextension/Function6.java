package ru.hirus.libextension;

@FunctionalInterface
public interface Function6<A, B, C, D, R> {
    R apply(A a, B b, C c, D d);
}
