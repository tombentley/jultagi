/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationCompositionStyle;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@PlanningSolution
public class Assignment {

    private Set<Broker> brokers = new TreeSet<>();
    private List<Partition> partitions;
    private List<Disk> disks;
    private HardSoftScore score;
    private List<Replica> assignments;

    /**
     * All the available brokers in the problem
     */
    public Set<Broker> getBrokers() {
        return brokers;
    }

    /**
     * All the available partitions in the problem
     */
//    @ProblemFactCollectionProperty
//    @ValueRangeProvider(id = "partitionsRange")
    public List<Partition> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<Partition> partitions) {
        this.partitions = partitions;
    }

    /**
     * All the available disks in the problem
     * @return
     */
    @ValueRangeProvider(id = "disksRange")
    @ProblemFactCollectionProperty
    public List<Disk> getDisks() {
        return disks;
    }

    public void setDisks(List<Disk> disks) {
        this.disks = disks;
        for (Disk d : disks) {
            this.brokers.add(d.getBroker());
        }
    }

    /**
     * The replica assignments
     */
    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "assignmentsRange")
    public List<Replica> getAssignments() {
        return assignments;
    }

//    //@PlanningEntityProperty
//    @ValueRangeProvider(id = "leaderRange")
//    public List<Boolean> getLeaderRange() {
//        return Arrays.asList(Boolean.FALSE, Boolean.TRUE);
//    }

    public void setAssignments(List<Replica> assignments) {
        this.assignments = assignments;
    }

    @PlanningScore
    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public static void main(String[] args) {
        int numBrokers = 3;
        int numRacks = 2;
        int disksPerBroker = 2;
        int numTopics = 10;
        long diskCapacity = UnitPrefix.TERA.times(1);
        int minPartitionsPerTopic = 1;
        int maxPartitionsPerTopic = 12;
        int minRf = 1;
        int maxRf = 3;
        Random rng = new Random(0);

        List<Broker> brokers = new ArrayList<>(numBrokers);
        for (int i = 0; i < numBrokers; i++) {
            brokers.add(new Broker(i, Integer.toString(i % numRacks)));
        }
        List<Disk> disks = new ArrayList<>();
        for (Broker b : brokers) {
            for (int i = 0; i < disksPerBroker; i++) {
                disks.add(new Disk(i, b, diskCapacity));
            }
        }

        List<Partition> partitions = new ArrayList<>();
        List<Replica> replicas = new ArrayList<>();
        for (int j = 0; j < numTopics; j++) {
            String topicName = new String(Character.toChars((int) 'A' + j));
            int numPartitions = rng.nextInt(maxPartitionsPerTopic - minPartitionsPerTopic + 1) + minPartitionsPerTopic;
            int rf = rng.nextInt(maxRf - minRf + 1) + minRf;
            System.out.println("Topic " + topicName + " has " + numPartitions + " partitions and RF=" + rf);
            for (int p = 0; p < numPartitions; p++) {
                Partition partition = new Partition();
                partition.setTopic(topicName);
                partition.setPartitionId(p);
                partition.setDiskUsage(UnitPrefix.GIGA.times(10));
                partitions.add(partition);
                for (int i = 0; i < rf; i++) {
                    Replica pa = new Replica(replicas.size());
                    pa.setDisk(disks.get((j + p + i) % disks.size()));
                    pa.setLeader(i == 0);
                    pa.setPartition(partition);
                    replicas.add(pa);
                }
            }
        }
        System.out.println("Total numReplicas " + replicas.size());

        Assignment a = new Assignment();
        a.setDisks(disks);
        a.setPartitions(partitions);
        a.setAssignments(replicas);
        System.out.println(a.getBrokers());
        System.out.println(a.getDisks());
        System.out.println(a.getPartitions());
        print(a.getAssignments());

        Assignment solvedCloudBalance = solve(a);

        // Display the result
        System.out.println(solvedCloudBalance.getScore().getHardScore() >= 0 ? "Solved:" : "Not solved:");
        AssignmentScoreCalculator calculator = new AssignmentScoreCalculator();
        calculator.setNumRacks(numRacks(solvedCloudBalance));
        calculator.setDebug("true");
        calculator.calculateScore(solvedCloudBalance);
        System.out.println(solvedCloudBalance.getScore());
        print(solvedCloudBalance.getAssignments());


//        a.addBroker(broker);// Add a broker
//        a.removeBroker();// Remove a broker
//        a.addBrokerDisk(disk);// Add disk(s) to existing broker
//        a.addPartitions();// Add new partition (or whole topic)
//        a.addPartitionReplicas(topic, partitionId, 1);// Add/remove replicas to a partition
//        a.pinTopic(topic)

    }

    private static Assignment solve(Assignment a) {
        // Build the Solver
        TerminationConfig terminationConfig = new TerminationConfig();
        terminationConfig.setUnimprovedSecondsSpentLimit(20L);
        terminationConfig.setBestScoreLimit("0hard/0soft");
        terminationConfig.setTerminationCompositionStyle(TerminationCompositionStyle.OR);

        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(AssignmentScoreCalculator.class);
        //scoreDirectorFactoryConfig.setInitializingScoreTrend("ONLY_DOWN");
        Map<String, String> scoreCalculatorConfig = new HashMap<>();
        scoreCalculatorConfig.put("debug", "false");
        scoreCalculatorConfig.put("numRacks", numRacks(a));
        scoreDirectorFactoryConfig.setEasyScoreCalculatorCustomProperties(scoreCalculatorConfig);

        SolverFactory<Assignment> empty = SolverFactory.createEmpty();
        SolverConfig solverConfig = empty.getSolverConfig();
        solverConfig.setEntityClassList(Arrays.asList(Replica.class));
        solverConfig.setSolutionClass(Assignment.class);
        solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);
        solverConfig.setTerminationConfig(terminationConfig);
        //solverConfig.setMoveThreadCount("AUTO");

        Solver<Assignment> solver = empty.buildSolver();

        // Solve the problem


        return solver.solve(a);
    }

    private static String numRacks(Assignment a) {
        Set<String> racks = new HashSet<>();
        for (Broker broker : a.getBrokers()) {
            racks.add(broker.getRack());
        }
        return Integer.toString(racks.size());
    }

    private static void print(Collection<Replica> solvedCloudBalance) {
        for (Map.Entry<Partition, List<Replica>> e : groupByPartition(solvedCloudBalance)) {
            System.out.print(e.getKey() + ": ");
            for (Replica pa : e.getValue()) {
                if (Boolean.TRUE.equals(pa.getLeader())) {
                    System.out.print("Disk " + pa.getDisk().getId() + " " + pa.getDisk().getBroker()+ ", ");
                }
            }
            for (Replica pa : e.getValue()) {
                if (Boolean.FALSE.equals(pa.getLeader())) {
                    System.out.print("Disk " + pa.getDisk().getId() + " " + pa.getDisk().getBroker()+ ", ");
                }
            }
            System.out.println();
        }
    }

    private static Set<Map.Entry<Partition, List<Replica>>> groupByPartition(Collection<Replica> solvedCloudBalance) {
        return solvedCloudBalance
                .stream()
                .collect(Collectors.groupingBy(pa -> pa.getPartition(), TreeMap::new, Collectors.toList())).entrySet();
    }
}
