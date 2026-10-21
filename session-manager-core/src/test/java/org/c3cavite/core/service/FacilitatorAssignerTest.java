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

    @Test
    void separatedModeRejectsGenderMismatchForLeadersAndAssistants() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        List<Facilitator> roster = List.of(
                new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS)),
                new Facilitator("Assistant", Role.ASSISTANT, Gender.FEMALE, List.of(AgeTier.TEENS)));

        assigner.assign(List.of(group), roster, SessionConfig.builder().capSize(6).build());

        assertNull(group.getLeader());
        assertNull(group.getAssistant());
    }

    @Test
    void duplicateRosterEntryCannotStaffTwoGroups() {
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));
        List<Group> groups = List.of(new Group(Gender.MALE, AgeTier.TEENS, null),
                new Group(Gender.MALE, AgeTier.TEENS, null));

        assertThrows(IllegalArgumentException.class, () -> assigner.assign(groups, List.of(leader, leader),
                SessionConfig.builder().capSize(6).build()));
    }

    @Test
    void explicitGenderExceptionOnlyWaivesGender() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));

        List<String> warnings = assigner.assign(List.of(group), List.of(leader),
                SessionConfig.builder().capSize(6).build(), List.of(new FacilitatorException(leader, false, true)));

        assertEquals(leader, group.getLeader());
        assertTrue(warnings.stream().anyMatch(w -> w.contains("exception") && w.contains("gender")));
    }

    @Test
    void qualificationAndGenderExceptionsAreIndependent() {
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.YOUTH));
        SessionConfig config = SessionConfig.builder().capSize(6).build();
        for (FacilitatorException exception : List.of(new FacilitatorException(leader, true, false),
                new FacilitatorException(leader, false, true))) {
            Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
            assigner.assign(List.of(group), List.of(leader), config, List.of(exception));
            assertNull(group.getLeader());
        }
        Group allowed = new Group(Gender.MALE, AgeTier.TEENS, null);
        assigner.assign(List.of(allowed), List.of(leader), config,
                List.of(new FacilitatorException(leader, true, true)));
        assertEquals(leader, allowed.getLeader());
    }

    @Test
    void exceptionDoesNotPromoteAssistantToLeader() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator assistant = new Facilitator("Assistant", Role.ASSISTANT, Gender.FEMALE, List.of());

        assigner.assign(List.of(group), List.of(assistant), SessionConfig.builder().capSize(6).build(),
                List.of(new FacilitatorException(assistant, true, true)));

        assertNull(group.getLeader());
        assertEquals(assistant, group.getAssistant());
    }

    @Test
    void normalEligibleStaffTakePriorityOverAnException() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator exceptionLeader = new Facilitator("Exception", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));
        Facilitator eligible = new Facilitator("Eligible", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));

        List<String> warnings = assigner.assign(List.of(group), List.of(exceptionLeader, eligible),
                SessionConfig.builder().capSize(6).build(),
                List.of(new FacilitatorException(exceptionLeader, false, true)));

        assertEquals(eligible, group.getLeader());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void mixedModeAllowsEitherGenderWithoutAnException() {
        Group group = new Group(null, AgeTier.TEENS, null);
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));

        assigner.assign(List.of(group), List.of(leader),
                SessionConfig.builder().capSize(6).genderMode(GenderMode.MIXED).build());

        assertEquals(leader, group.getLeader());
    }

    @Test
    void exceptionsMustReferToUniqueRosterInstances() {
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));
        Facilitator other = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));
        FacilitatorException exception = new FacilitatorException(leader, true, false);
        SessionConfig config = SessionConfig.builder().capSize(6).build();

        assertThrows(IllegalArgumentException.class, () -> assigner.assign(List.of(), List.of(other), config,
                List.of(exception)));
        assertThrows(IllegalArgumentException.class, () -> assigner.assign(List.of(), List.of(leader), config,
                List.of(exception, exception)));
    }

    @Test
    void reusingGroupsDoesNotRetainStaffWhoseExceptionWasRemoved() {
        Group group = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));
        Facilitator assistant = new Facilitator("Assistant", Role.ASSISTANT, Gender.FEMALE, List.of(AgeTier.TEENS));
        SessionConfig config = SessionConfig.builder().capSize(6).build();
        List<Facilitator> roster = List.of(leader, assistant);
        assigner.assign(List.of(group), roster, config, List.of(
                new FacilitatorException(leader, false, true), new FacilitatorException(assistant, false, true)));
        assertEquals(leader, group.getLeader());
        assertEquals(assistant, group.getAssistant());

        assigner.assign(List.of(group), roster, config);

        assertNull(group.getLeader());
        assertNull(group.getAssistant());
    }

    @Test
    void fullNormalCoverageDoesNotUseAnUnnecessaryException() {
        Group youth = new Group(Gender.MALE, AgeTier.YOUTH, null);
        Group teens = new Group(Gender.MALE, AgeTier.TEENS, null);
        Facilitator flexible = new Facilitator("Flexible", Role.LEADER, Gender.MALE,
                List.of(AgeTier.YOUTH, AgeTier.TEENS));
        Facilitator exception = new Facilitator("Exception", Role.LEADER, Gender.FEMALE, List.of(AgeTier.YOUTH));
        Facilitator teensOnly = new Facilitator("Teens only", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));

        List<String> warnings = assigner.assign(List.of(youth, teens), List.of(flexible, exception, teensOnly),
                SessionConfig.builder().capSize(6).build(), List.of(new FacilitatorException(exception, false, true)));

        assertTrue(warnings.isEmpty());
        assertEquals(flexible, youth.getLeader());
        assertEquals(teensOnly, teens.getLeader());
    }
}
