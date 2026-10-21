package org.c3cavite.core.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupTest {
    @Test
    void leaderAndAssistantSlotsRejectWrongRoles() {
        Group group = new Group(Gender.MALE, AgeTier.YOUTH, null);
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH));
        Facilitator assistant = new Facilitator("Assistant", Role.ASSISTANT, Gender.MALE, List.of(AgeTier.YOUTH));
        assertThrows(IllegalArgumentException.class, () -> group.assignLeader(assistant));
        assertThrows(IllegalArgumentException.class, () -> group.assignAssistant(leader));
    }

    @Test
    void nullAttendeesCannotEnterAGroup() {
        Group group = new Group(Gender.MALE, AgeTier.YOUTH, null);
        assertThrows(IllegalArgumentException.class, () -> group.addParticipant(null));
    }

    @Test
    void sizeCountsOnlyParticipantsAndSetterAliasesEnforceRoles() {
        Group group = new Group(Gender.MALE, AgeTier.YOUTH, null);
        group.addParticipant(new Participant("Attendee", 17, Gender.MALE, null));
        Facilitator leader = new Facilitator("Leader", Role.LEADER, Gender.MALE, List.of(AgeTier.YOUTH));
        Facilitator assistant = new Facilitator("Assistant", Role.ASSISTANT, Gender.MALE, List.of(AgeTier.YOUTH));
        group.setLeader(leader);
        group.setAssistant(assistant);
        assertEquals(1, group.getSize());
        assertEquals(leader, group.getLeader());
        assertEquals(assistant, group.getAssistant());
        assertThrows(IllegalArgumentException.class, () -> group.setLeader(assistant));
        assertThrows(IllegalArgumentException.class, () -> group.setAssistant(leader));
        group.setLeader(null);
        group.setAssistant(null);
        assertNull(group.getLeader());
        assertNull(group.getAssistant());
    }
}
