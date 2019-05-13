/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The fitness function for the optimization.
 */
/*
TODO We should really really use an incremental score because it's much much more efficient
but it's a lot harder to write (and thus verify)
 */
public class AssignmentScoreCalculator implements EasyScoreCalculator<Assignment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssignmentScoreCalculator.class);

    private boolean debug;
    private int numRacks;

    public AssignmentScoreCalculator() {
        this(false);
    }

    public AssignmentScoreCalculator(boolean debug) {
        this.debug = debug;
    }

    public void setDebug(String debug) {
        this.debug = Boolean.getBoolean(debug);
    }

    public String getDebug() {
        return Boolean.toString(this.debug);
    }

    public void setNumRacks(String numRacks) {
        if (numRacks != null) {
            this.numRacks = Integer.parseInt(numRacks);
        }
    }

    /**
     * This method is only called if the {@link Score} cannot be predicted.
     * The {@link Score} can be predicted for example after an undo {@link Move}.
     *
     * @param assignment never null
     * @return never null
     */
    @Override
    public Score calculateScore(Assignment assignment) {
        int brokerUniqueness = 0;
        int rackUniqueness = 0;
        // The unique leader isn't directly part of the score, but it affects how we calculate the score
//        int uniqueLeader = 0;
        int overCommittedDisk = 0;
        int veryOverCommittedDisk = 0;

        Map<Disk, Long> diskUsage = new HashMap<>();

        Map<Partition, Integer> rfs = new HashMap<>();
        Map<Partition, Set<Broker>> partitionToBrokers = new HashMap<>();
        Map<Partition, Map<String, Integer>> partitionToRack = new HashMap<>();
        Map<Partition, Broker> partitionToLeader = new HashMap<>();
        for (Replica pa : assignment.getAssignments()) {
            Partition p = pa.getPartition();

            Integer rf = rfs.get(p);
            if (rf == null) {
                rfs.put(p, 1);
            } else {
                rfs.put(p, rf + 1);
            }

            // Replicas are assigned to distinct brokers
            Set<Broker> assignedBrokers = partitionToBrokers.get(p);
            if (assignedBrokers == null) {
                assignedBrokers = new HashSet<>();
                partitionToBrokers.put(p, assignedBrokers);
            }
            Disk disk = pa.getDisk();
            Broker broker = disk.getBroker();
            if (assignedBrokers.contains(broker)) {
                if (debug) {
                    LOGGER.info("Same broker for partition {}: {} and {}, hard -= 1", p, broker, assignedBrokers);
                }
                brokerUniqueness -= 1;
            } else {
                assignedBrokers.add(broker);
            }

            // Replica brokers are in different racks
            Map<String, Integer> assignedRacks = partitionToRack.get(p);
            if (assignedRacks == null) {
                assignedRacks = new HashMap<>();
                partitionToRack.put(p, assignedRacks);
            }
            String rack = disk.getBroker().getRack();
            Integer rackCount = assignedRacks.get(rack);
            if (rackCount != null) {
                rackCount += 1;
                assignedRacks.put(rack, rackCount);
            } else {
                assignedRacks.put(rack, 1);
            }

            // We don't exceed disk capacity
            Long usage = diskUsage.get(disk);
            if (usage == null) {
                usage = 0L;
            }
            usage += p.getDiskUsage();
            diskUsage.put(disk, usage);

//            if (Boolean.TRUE.equals(pa.getLeader())) {
//                Broker leader = partitionToLeader.get(p);
//                if (leader == null) {
//                    Broker existingLeader = partitionToLeader.put(p, broker);
//                    if (existingLeader != null) {
//                        //if (debug) System.err.println("Duplicate leader");
//                        uniqueLeader -= 1;// already a leader
//                    }
//                }
//            }
        }

        for (Map.Entry<Partition, Map<String, Integer>> entry : partitionToRack.entrySet()) {
            Partition p = entry.getKey();
            Integer rf = rfs.get(p);
            for (Map.Entry<String, Integer> e2 : entry.getValue().entrySet()) {
                String rack = e2.getKey();
                Integer rackUsageCount = e2.getValue();
                //if (rf > numRacks) {
                    int replicasPerRack = (rf + numRacks - 1) / numRacks;
                    //if (rackUsageCount > replicasPerRack) {
                        int cost = Math.max(0, rackUsageCount - replicasPerRack);
                        rackUniqueness -= cost;
                        if (debug) {
                            LOGGER.info("Same rack for partition {}: {} is used {} times, soft -= {}", p, rack, rackUsageCount, cost);
                        }
                    //}
//                } else {
//                    rackUsageCount > 1
//                }
//
//                if (rackUsageCount > numRacks) {// TODO is this correct? Really it should be comparing RF with rackUsageCount
//                    rackUniqueness -= rackUsageCount - numRacks;
//                    if (debug) {
//                        LOGGER.info("Same rack for partition {}: {} is used {} times, soft -= 1", p, rack, rackUsageCount);
//                    }
//                }rackUsageCount
            }
        }

        for (Map.Entry<Disk, Long> entry : diskUsage.entrySet()) {
            Disk disk = entry.getKey();
            Long usage = entry.getValue();
            if (usage > disk.getCapacity()) {
                veryOverCommittedDisk -= 1;
                if (debug) {
                    LOGGER.info("Impossibly overcommitted disk {} {}%, hard -= 1", disk, (usage * 100.0) / disk.getCapacity() + "%");
                }
            } else if (usage > disk.getCapacity() * 0.8) {
                double usagePercent = Math.round((usage / disk.getCapacity() - 0.8) * 100.0);
                int cost = (int) (usagePercent / 2);
                overCommittedDisk -= cost;
                if (debug) {
                    LOGGER.info("Overcommitted disk {} {}%, soft -= {}", disk , (usage * 100.0) / disk.getCapacity(), cost);
                }
            }
        }

        // partitions without a leader
//        int hard1 = partitionToBrokers.size() - partitionToLeader.size();
//        if (hard1 != 0) {
//            Set<Partition> partitions = new HashSet<>(partitionToBrokers.keySet());
//            partitions.removeAll(partitionToLeader.keySet());
//            uniqueLeader -= hard1;
//        }


        return HardSoftScore.of(brokerUniqueness + veryOverCommittedDisk, rackUniqueness + overCommittedDisk);
    }
}
