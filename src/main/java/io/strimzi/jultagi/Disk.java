/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import org.optaplanner.core.api.domain.lookup.PlanningId;

/**
 * A disk within a {@linkplain #getBroker()}  broker}.
 * A disk is identified by an {@linkplain #getId() id}, which should be unique with the broker
 * (though it's used only for labelling purposes, and does not affect {@code Object.equals()} etc.).
 * A disk also has a {@linkplain #getCapacity() capacity}, measured in bytes.
 * Technically this is the capacity of the filesystem on the physical disk.
 */
public class Disk {

    private int id;
    private Broker broker;
    private long capacity;

    public Disk(int id, Broker broker, long capacity) {
        this.id = id;
        this.broker = broker;
        this.capacity = capacity;
    }

    @PlanningId
    public String getId2() {
        return broker.getId() + "/" + id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "Disk{" +
                "id=" + id +
                ", broker=" + broker +
                ", capacity=" + capacity +
                '}';
    }
}
