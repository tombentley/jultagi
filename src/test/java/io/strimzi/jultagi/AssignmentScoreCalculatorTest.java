/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.jultagi;

import org.junit.Test;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class AssignmentScoreCalculatorTest {

    @Test
    public void testReplicasSameBroker() {
        Assignment a = new Assignment();
        Broker broker = new Broker(0, "0");
        Disk disk = new Disk(0, broker, 100);
        a.setDisks(singletonList(disk));
        Partition p = new Partition("A", 0, 1);
        a.setPartitions(singletonList(p));
        AssignmentScoreCalculator c = new AssignmentScoreCalculator();
        c.setNumRacks("1");

        a.setAssignments(asList(new Replica(0, true, p, disk), new Replica(1, false, p, disk)));
        assertEquals(HardSoftScore.of(-1, 0), c.calculateScore(a));
    }

    @Test
    public void testReplicasSameRack() {
        Assignment a = new Assignment();
        Broker broker0 = new Broker(0, "0");
        Broker broker1 = new Broker(1, "0");
        Broker broker2 = new Broker(2, "0");
        Broker broker3 = new Broker(3, "1");
        Broker broker4 = new Broker(4, "1");
        Disk disk0 = new Disk(0, broker0, 100);
        Disk disk1 = new Disk(0, broker1, 100);
        Disk disk2 = new Disk(0, broker2, 100);
        Disk disk3 = new Disk(0, broker3, 100);
        Disk disk4 = new Disk(0, broker4, 100);
        a.setDisks(asList(disk0, disk1, disk2, disk3, disk4));
        Partition p = new Partition("A", 0, 1);
        a.setPartitions(singletonList(p));
        AssignmentScoreCalculator c = new AssignmentScoreCalculator(true);
        c.setNumRacks("2");

        // RF = 2
        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk1)));
        assertEquals(HardSoftScore.of(0, -1), c.calculateScore(a));

        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk2)));
        assertEquals(HardSoftScore.of(0, -1), c.calculateScore(a));

        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk3)));
        assertEquals(HardSoftScore.of(0, 0), c.calculateScore(a));

        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk4)));
        assertEquals(HardSoftScore.of(0, 0), c.calculateScore(a));

        // RF = 4
        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk1),
                new Replica(2, true, p, disk2),
                new Replica(3, false, p, disk3)));
        assertEquals(HardSoftScore.of(0, -1), c.calculateScore(a));

        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk1),
                new Replica(2, true, p, disk3),
                new Replica(3, false, p, disk4)));
        assertEquals(HardSoftScore.of(0, 0), c.calculateScore(a));

        // RF = 5
        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk1),
                new Replica(2, true, p, disk2),
                new Replica(3, false, p, disk3),
                new Replica(4, false, p, disk4)));
        assertEquals(HardSoftScore.of(0, 0), c.calculateScore(a));

        a.setAssignments(asList(
                new Replica(0, true, p, disk0),
                new Replica(1, false, p, disk1),
                new Replica(2, false, p, disk2),
                new Replica(3, true, p, disk3),
                new Replica(4, false, p, disk4)));
        assertEquals(HardSoftScore.of(0, 0), c.calculateScore(a));

        // TODO test that, e.g. RF = 5 and two racks we correctly score the possible assignments
    }

    @Test
    public void testDiskOvercommitted() {
        Assignment a = new Assignment();
        Broker broker0 = new Broker(0, "0");
        Broker broker1 = new Broker(1, "1");
        Disk disk0 = new Disk(0, broker0, 10);
        Disk disk1 = new Disk(0, broker1, 10);
        a.setDisks(asList(disk0, disk1));
        Partition p0 = new Partition("A", 0, 5);
        Partition p1 = new Partition("A", 1, 5);
        Partition p2 = new Partition("A", 2, 1);
        a.setPartitions(asList(p0, p1));
        AssignmentScoreCalculator c = new AssignmentScoreCalculator(true);
        c.setNumRacks("1");

        // between 80 and 100% usage is soft
        a.setAssignments(asList(new Replica(0, true, p0, disk0), new Replica(1, false, p1, disk0)));
        assertEquals(HardSoftScore.of(0, -10), c.calculateScore(a));

        // > 100% usage is hard
        a.setAssignments(asList(new Replica(0, true, p0, disk0), new Replica(1, false, p1, disk0), new Replica(2, false, p2, disk0)));
        assertEquals(HardSoftScore.of(-1, 0), c.calculateScore(a));

        a.setAssignments(asList(new Replica(0, true, p0, disk0), new Replica(1, false, p1, disk1), new Replica(2, false, p2, disk0)));
        assertEquals(HardSoftScore.of(0, 0), c.calculateScore(a));
    }
}
