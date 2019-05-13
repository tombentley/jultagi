/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import java.util.Comparator;
import java.util.Objects;

/**
 * A partition of a topic.
 * A partition is identified by its {@linkplain #getTopic() topic name} and the {@linkplain #getPartitionId() partition id}.
 * A partition requires an amount of {@linkplain #getDiskUsage() persistent storage}.
 * Solutions will try to ensure assigned brokers have enough disk space for all their assigned partitions.
 */
public class Partition implements Comparable<Partition> {
    public static final Comparator<Partition> PARTITION_COMPARATOR = Comparator
            .<Partition, String>comparing(p -> p.topic)
            .thenComparing(p -> p.partitionId);

    public Partition() {
    }

    public Partition(String topic, int partitionId, long diskUsage) {
        this.topic = topic;
        this.partitionId = partitionId;
        this.diskUsage = diskUsage;
    }

    private String topic;
    private int partitionId;
    private long diskUsage;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    public long getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(long diskUsage) {
        this.diskUsage = diskUsage;
    }

    @Override
    public String toString() {
        return topic + '/' + partitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Partition partition = (Partition) o;
        return partitionId == partition.partitionId &&
                Objects.equals(topic, partition.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, partitionId);
    }

    @Override
    public int compareTo(Partition o) {
        return PARTITION_COMPARATOR.compare(this, o);
    }
}
