package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacilitatorAssignerTest {

    private final FacilitatorAssigner assigner = new FacilitatorAssigner();

    @Test
    @DisplayName("Given qualified leader, when assign is called, then group should have leader")
    void testAssignLeader() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator leader = new Facilitator("Leader 1", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));
        SessionConfig config = SessionConfig.create(GenderMode.SEPARATED, 5, 8, List.of(AgeTier.TEENS), false);

        List<String> warnings = assigner.assign(List.of(group), List.of(leader), config);

        assertEquals(leader, group.getLeader());
        assertTrue(warnings.isEmpty());
    }

    @Test
    @DisplayName("Given no qualified leader, when assign is called, then group remains leaderless and warning added")
    void testAssignNoLeader() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        SessionConfig config = SessionConfig.create(GenderMode.SEPARATED, 5, 8, List.of(AgeTier.TEENS), false);

        List<String> warnings = assigner.assign(List.of(group), List.of(), config);

        assertNull(group.getLeader());
        assertFalse(warnings.isEmpty());
        assertTrue(warnings.get(0).contains("leaderless"));
    }

    @Test
    @DisplayName("Given gender mismatch leader, when assign is called, then still assign but prefer match if available")
    void testAssignGenderPreference() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator femaleLeader = new Facilitator("Female Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));
        Facilitator maleLeader = new Facilitator("Male Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));
        SessionConfig config = SessionConfig.create(GenderMode.SEPARATED, 5, 8, List.of(AgeTier.TEENS), false);

        // Even if femaleLeader is first in list, it should pick maleLeader due to preference
        assigner.assign(List.of(group), List.of(femaleLeader, maleLeader), config);

        assertEquals(maleLeader, group.getLeader());
    }
}
