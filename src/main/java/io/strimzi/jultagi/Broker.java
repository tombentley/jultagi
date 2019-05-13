/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import java.util.Objects;

/**
 * Represents a broker within the Kafka cluster. Each broker must have a unique {@linkplain #getId() id}
 * and may have a rack.
 */
public class Broker implements Comparable<Broker> {
    private int id;
    private String rack;
    // TODO properties for pertinent broker facts, e.g. CPU, networkInBandwith, networkOutBandwith, cpu


    public Broker(int id, String rack) {
        this.id = id;
        this.rack = rack;
    }

    public int getId() {
        return id;
    }

    public String getRack() {
        return rack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Broker broker = (Broker) o;
        return id == broker.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Broker{" +
                "id=" + id +
                ", rack='" + rack + '\'' +
                '}';
    }

    @Override
    public int compareTo(Broker o) {
        return Integer.compare(this.id, o.id);
    }
}
