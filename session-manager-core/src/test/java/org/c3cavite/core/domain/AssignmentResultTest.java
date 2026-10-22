package org.c3cavite.core.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AssignmentResultTest {
    private final Participant attendee = new Participant("Attendee", 17, Gender.MALE, null);
    private final Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH));

    @Test
    void capturesListsRatherThanRetainingMutableInputs() {
        List<Group> groups = new ArrayList<>(List.of(staffedGroup()));
        List<UnassignedParticipant> unassigned = new ArrayList<>(List.of(new UnassignedParticipant(attendee, "Inactive")));
        List<String> warnings = new ArrayList<>(List.of("Check attendee"));
        AssignmentResult result = new AssignmentResult(groups, unassigned, warnings);

        groups.clear();
        unassigned.clear();
        warnings.clear();

        assertEquals(1, result.getTotalGroups());
        assertEquals(1, result.getTotalAssignedParticipants());
        assertEquals(1, result.getUnassignedParticipants().size());
        assertEquals(List.of("Check attendee"), result.getWarnings());
        assertThrows(UnsupportedOperationException.class, () -> result.getGroups().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getWarnings().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getUnassignedParticipants().clear());
    }

    @Test
    void capturesGroupContentsAndStaffing() {
        Group group = staffedGroup();
        AssignmentResult result = new AssignmentResult(List.of(group), List.of(), List.of());

        group.addParticipant(new Participant("Later attendee", 18, Gender.MALE, null));
        group.assignLeader(null);

        assertEquals(1, result.getTotalAssignedParticipants());
        assertEquals(leader, result.getGroups().get(0).getLeader());
    }

    @Test
    void returnedGroupsCannotBeChanged() {
        AssignmentResult result = new AssignmentResult(List.of(staffedGroup()), List.of(), List.of());
        Group returned = result.getGroups().get(0);

        assertThrows(UnsupportedOperationException.class, () -> returned.addParticipant(attendee));
        assertThrows(UnsupportedOperationException.class, () -> returned.assignLeader(null));
        assertThrows(UnsupportedOperationException.class, () -> returned.assignAssistant(leader));
    }

    @Test
    void facilitatorQualificationsCannotChangeAfterResultConstruction() {
        List<AgeTier> qualifications = new ArrayList<>(List.of(AgeTier.YOUTH));
        Group group = new Group(Gender.MALE, AgeTier.YOUTH, null);
        group.assignLeader(new Facilitator("Leader", Role.LEADER, Gender.MALE, qualifications));
        AssignmentResult result = new AssignmentResult(List.of(group), List.of(), List.of());

        qualifications.clear();

        assertEquals(List.of(AgeTier.YOUTH), result.getGroups().get(0).getLeader().getQualifiedTiers());
    }

    @Test
    void fullyStaffedAndEmptyAssignmentsAreFinal() {
        AssignmentResult staffed = new AssignmentResult(List.of(staffedGroup()), List.of(), List.of());
        AssignmentResult empty = new AssignmentResult(List.of(), List.of(), List.of());

        assertEquals(AssignmentState.FINAL, staffed.getState());
        assertTrue(staffed.isFinal());
        assertEquals(AssignmentState.FINAL, empty.getState());
        assertTrue(empty.getBlockers().isEmpty());
    }

    @Test
    void leaderlessGroupsHaveActionableBlockersWithGroupNumbers() {
        Group unstaffed = new Group(Gender.FEMALE, AgeTier.TEENS, null);
        AssignmentResult result = new AssignmentResult(List.of(staffedGroup(), unstaffed), List.of(), List.of());

        assertEquals(AssignmentState.NON_FINAL_PROPOSAL, result.getState());
        assertFalse(result.isFinal());
        AssignmentBlocker blocker = result.getBlockers().get(0);
        assertEquals(AssignmentBlocker.Code.MISSING_LEADER, blocker.getCode());
        assertEquals(2, blocker.getGroupNumber());
        assertTrue(blocker.getMessage().contains("Assign a LEADER"));
        assertTrue(blocker.getMessage().contains("TEENS"));
        assertTrue(blocker.getMessage().contains("FEMALE"));
        assertThrows(UnsupportedOperationException.class, () -> result.getBlockers().clear());
    }

    @Test
    void aRejectedAssistantDoesNotReplaceTheExistingLeader() {
        Group group = staffedGroup();
        assertThrows(IllegalArgumentException.class, () -> group.assignLeader(
                new Facilitator("Assistant", Role.ASSISTANT, Gender.MALE, List.of(AgeTier.YOUTH))));
        AssignmentResult result = new AssignmentResult(List.of(group), List.of(), List.of());

        assertTrue(result.isFinal());
        assertEquals(leader, result.getGroups().get(0).getLeader());
    }

    @Test
    void overCapGroupIsAProposalEvenWhenStaffed() {
        Group group = staffedGroup();
        group.addParticipant(new Participant("Another attendee", 18, Gender.MALE, null));
        AssignmentResult result = new AssignmentResult(List.of(group), List.of(), List.of(), 1);

        assertFalse(result.isFinal());
        assertEquals(AssignmentBlocker.Code.GROUP_CAP_EXCEEDED, result.getBlockers().get(0).getCode());
        assertTrue(result.getBlockers().get(0).getMessage().contains("cap of 1"));
    }

    @Test
    void excludedAttendeesRetainReasonsWithoutBlockingStaffedGroups() {
        AssignmentResult result = new AssignmentResult(List.of(staffedGroup()),
                List.of(new UnassignedParticipant(attendee, "UNKNOWN_TIER")), List.of("Check age"));

        assertTrue(result.isFinal());
        assertEquals("UNKNOWN_TIER", result.getUnassignedParticipants().get(0).reason());
        assertEquals(List.of("Check age"), result.getWarnings());
    }

    private Group staffedGroup() {
        Group group = new Group(Gender.MALE, AgeTier.YOUTH, null);
        group.addParticipant(attendee);
        group.assignLeader(leader);
        return group;
    }
}
