package org.c3cavite.core.service;

import org.c3cavite.core.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GroupAssignmentServiceTest {

    private final GroupAssignmentService service = new GroupAssignmentService();

    @Test
    void filtersAndAssignsUsingTheCurrentSessionsAgeBoundaries() {
        SessionConfig config = SessionConfig.builder().capSize(6).activeTiers(List.of(AgeTier.TEENS))
                .ageTierBoundaries(Map.of(AgeTier.TEENS, new AgeRange(13, 17),
                        AgeTier.YOUTH, new AgeRange(18, 25),
                        AgeTier.YOUNG_ADULT, new AgeRange(26, Integer.MAX_VALUE))).build();
        Participant attendee = new Participant("Attendee", 17, Gender.MALE, null);
        Participant inactive = new Participant("Inactive", 18, Gender.MALE, null);
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS));

        AssignmentResult result = service.assign(config, List.of(attendee, inactive), List.of(leader));

        assertEquals(1, result.getTotalAssignedParticipants());
        assertEquals(List.of(attendee), result.getGroups().get(0).getParticipants());
        assertEquals(AgeTier.TEENS, result.getGroups().get(0).getAgeTierTarget());
        assertEquals(AgeTier.TEENS, result.getGroups().get(0).getParticipants().get(0).getAgeTier());
        assertEquals(AgeTier.YOUTH, attendee.getAgeTier());
        assertEquals(leader, result.getGroups().get(0).getLeader());
        assertEquals(inactive, result.getUnassignedParticipants().get(0).participant());
    }

    @Test
    void reportsUnknownTierOutsideConfiguredBoundaries() {
        SessionConfig config = SessionConfig.builder().capSize(6).ageTierBoundaries(Map.of(
                AgeTier.TEENS, new AgeRange(14, 17), AgeTier.YOUTH, new AgeRange(18, 25),
                AgeTier.YOUNG_ADULT, new AgeRange(26, 70))).build();
        Participant attendee = new Participant("Outside range", 71, Gender.MALE, LifeStage.STUDENT);

        AssignmentResult result = service.assign(config, List.of(attendee), List.of());

        assertEquals(0, result.getTotalAssignedParticipants());
        assertEquals("UNKNOWN_TIER", result.getUnassignedParticipants().get(0).reason());
        assertNull(result.getUnassignedParticipants().get(0).participant().getAgeTier());
    }

    @Test
    void reusingInputInAnotherSessionDoesNotChangeEarlierResults() {
        Participant attendee = new Participant("Attendee", 17, Gender.MALE, null);
        SessionConfig custom = SessionConfig.builder().capSize(6).ageTierBoundaries(Map.of(
                AgeTier.TEENS, new AgeRange(13, 17), AgeTier.YOUTH, new AgeRange(18, 25),
                AgeTier.YOUNG_ADULT, new AgeRange(26, Integer.MAX_VALUE))).build();

        AssignmentResult customResult = service.assign(custom, List.of(attendee), List.of());
        AssignmentResult defaultResult = service.assign(SessionConfig.builder().capSize(6).build(),
                List.of(attendee), List.of());

        assertEquals(AgeTier.TEENS, customResult.getGroups().get(0).getParticipants().get(0).getAgeTier());
        assertEquals(AgeTier.YOUTH, defaultResult.getGroups().get(0).getParticipants().get(0).getAgeTier());
        assertEquals(AgeTier.YOUTH, attendee.getAgeTier());
    }

    @Test
    @DisplayName("Given valid participants and facilitators, when assign is called, then return successful result")
    void testHappyPath() {
        SessionConfig config = SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.TEENS), false);
        List<Participant> participants = List.of(
                new Participant("P1", 14, Gender.MALE, null),
                new Participant("P2", 14, Gender.FEMALE, null)
        );
        List<Facilitator> facilitators = List.of(
                new Facilitator("L1", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS))
        );

        AssignmentResult result = service.assign(config, participants, facilitators);

        assertEquals(1, result.getTotalGroups());
        assertEquals(2, result.getTotalAssignedParticipants());
        assertEquals(2, result.getGroups().get(0).getParticipants().size());
        assertEquals(AssignmentState.FINAL, result.getState());
    }

    @Test
    @DisplayName("Given underage participant, when assign is called, then move to unassigned")
    void testUnderageParticipant() {
        SessionConfig config = SessionConfig.create(GenderMode.MIXED, 5, 8, List.of(AgeTier.TEENS), false);
        List<Participant> participants = List.of(
                new Participant("Child", 12, Gender.MALE, null)
        );

        AssignmentResult result = service.assign(config, participants, List.of());

        assertEquals(0, result.getTotalAssignedParticipants());
        assertEquals(1, result.getUnassignedParticipants().size());
        assertTrue(result.getUnassignedParticipants().get(0).reason().contains("under 13"));
    }

    @Test
    @DisplayName("Given leader shortage, when assign is called, then consolidate groups up to capSize")
    void testConsolidation() {
        SessionConfig config = SessionConfig.create(GenderMode.MIXED, 3, 10, List.of(AgeTier.TEENS), false);
        List<Participant> participants = List.of(
                new Participant("P1", 14, Gender.MALE, null),
                new Participant("P2", 14, Gender.MALE, null),
                new Participant("P3", 14, Gender.MALE, null),
                new Participant("P4", 14, Gender.MALE, null),
                new Participant("P5", 14, Gender.MALE, null),
                new Participant("P6", 14, Gender.MALE, null),
                new Participant("P7", 14, Gender.MALE, null),
                new Participant("P8", 14, Gender.MALE, null),
                new Participant("P9", 14, Gender.MALE, null),
                new Participant("P10", 14, Gender.MALE, null)
        );
        List<Facilitator> facilitators = List.of(
                new Facilitator("L1", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS)),
                new Facilitator("L2", Role.LEADER, Gender.MALE, List.of(AgeTier.TEENS))
        );

        AssignmentResult result = service.assign(config, participants, facilitators);

        assertEquals(2, result.getTotalGroups());
        assertEquals(5, result.getGroups().get(0).getParticipants().size());
        assertEquals(5, result.getGroups().get(1).getParticipants().size());
    }

    @Test
    void shortageReturnsAProposalWithAGroupBlocker() {
        AssignmentResult result = service.assign(SessionConfig.builder().capSize(6).build(),
                List.of(new Participant("Attendee", 17, Gender.MALE, null)), List.of());

        assertEquals(AssignmentState.NON_FINAL_PROPOSAL, result.getState());
        assertEquals(AssignmentBlocker.Code.MISSING_LEADER, result.getBlockers().get(0).getCode());
        assertEquals(1, result.getBlockers().get(0).getGroupNumber());
    }

    @Test
    void emptyInputReturnsAnEmptyFinalResult() {
        AssignmentResult result = service.assign(SessionConfig.builder().capSize(6).build(), List.of(), List.of());

        assertTrue(result.isFinal());
        assertTrue(result.getGroups().isEmpty());
        assertTrue(result.getBlockers().isEmpty());
    }

    @Test
    void genderMismatchCannotSilentlyFinalizeASeparatedGroup() {
        AssignmentResult result = service.assign(SessionConfig.builder().capSize(6).build(),
                List.of(new Participant("Attendee", 17, Gender.MALE, null)),
                List.of(new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.YOUTH))));

        assertFalse(result.isFinal());
        assertNull(result.getGroups().get(0).getLeader());
    }

    @Test
    void explicitExceptionsResolveShortageOnlyForTheCurrentRun() {
        SessionConfig config = SessionConfig.builder().capSize(6).build();
        List<Participant> participants = List.of(new Participant("Attendee", 17, Gender.MALE, null));
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.FEMALE, List.of(AgeTier.TEENS));

        AssignmentResult allowed = service.assign(config, participants, List.of(leader),
                List.of(new FacilitatorException(leader, true, true)));
        AssignmentResult unchanged = service.assign(config, participants, List.of(leader));

        assertTrue(allowed.isFinal());
        assertEquals(leader, allowed.getGroups().get(0).getLeader());
        assertFalse(unchanged.isFinal());
        assertTrue(allowed.getWarnings().stream().anyMatch(w -> w.contains("exception")));
        assertEquals(List.of(AgeTier.TEENS), leader.getQualifiedTiers());
    }
}
