/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.Objects;

/**
 * The assignment of a single partition to a single broker.
 */
@PlanningEntity
public class Replica {
    private Integer id;
    private Boolean leader;
    private Partition partition;
    private Disk disk;

    public Replica() {
    }

    public Replica(int id) {
        this.id = id;
    }

    public Replica(Integer id, Boolean leader, Partition partition, Disk disk) {
        this.id = id;
        this.leader = leader;
        this.partition = partition;
        this.disk = disk;
    }

    @PlanningId
    public Integer getId() {
        return id;
    }

    //@PlanningVariable(valueRangeProviderRefs = {"leaderRange"})
    public Boolean getLeader() {
        return leader;
    }

    public void setLeader(Boolean leader) {
        this.leader = leader;
    }

    //@PlanningVariable(valueRangeProviderRefs = {"partitionsRange"})
    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    @PlanningVariable(valueRangeProviderRefs = {"disksRange"})
    public Disk getDisk() {
        return disk;
    }

    public void setDisk(Disk disk) {
        this.disk = disk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Replica that = (Replica) o;
        return leader == that.leader &&
                Objects.equals(partition, that.partition) &&
                Objects.equals(disk, that.disk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leader, partition, disk);
    }

    @Override
    public String toString() {
        return "PartitionAssignment{" +
                "leader=" + leader +
                ", partition=" + partition +
                ", disk=" + disk +
                '}';
    }
}
