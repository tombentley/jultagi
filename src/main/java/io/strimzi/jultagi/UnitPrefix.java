/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

/**
 * Helper class for SI unit prefixes (decimal and binary).
 */
public enum UnitPrefix {
    KILO(1000L),
    KIBI(1024L),
    MEGA(1_000_000L),
    MEBI(1024 * 1024L),
    GIGA(1_000_000_000L),
    GIBI(1024 * 1024 * 1024L),
    TERA(1_000_000_000_000L),
    TEBI(1024 * 1024 * 1024 * 1024L);

    private final long factor;

    UnitPrefix(long factor) {
        this.factor = factor;
    }

    long times(long withPrefix) {
        return factor * withPrefix;
    }

    double from(long withoutPrefix) {
        return ((double) withoutPrefix) / factor;
    }
}
