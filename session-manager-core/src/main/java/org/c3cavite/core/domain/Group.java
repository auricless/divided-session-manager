package org.c3cavite.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group {
    private final List<Participant> participants = new ArrayList<>();
    private Facilitator leader;
    private Facilitator assistant;
    private final boolean readOnly;
    
    // Metadata for tracking/validation
    private final Gender genderTarget;
    private final AgeTier ageTierTarget;
    private final LifeStage lifeStageTarget;

    public Group(Gender genderTarget, AgeTier ageTierTarget, LifeStage lifeStageTarget) {
        this(genderTarget, ageTierTarget, lifeStageTarget, false);
    }

    private Group(Gender genderTarget, AgeTier ageTierTarget, LifeStage lifeStageTarget, boolean readOnly) {
        this.genderTarget = genderTarget;
        this.ageTierTarget = ageTierTarget;
        this.lifeStageTarget = lifeStageTarget;
        this.readOnly = readOnly;
    }

    Group immutableCopy() {
        Group copy = new Group(genderTarget, ageTierTarget, lifeStageTarget, true);
        copy.participants.addAll(participants);
        copy.leader = leader;
        copy.assistant = assistant;
        return copy;
    }

    private void requireMutable() {
        if (readOnly) throw new UnsupportedOperationException("Assignment result groups are immutable");
    }

    public void addParticipant(Participant p) {
        requireMutable();
        if (p == null) throw new IllegalArgumentException("Participant is required");
        participants.add(p);
    }

    public void assignLeader(Facilitator f) {
        requireMutable();
        if (f != null && f.getRole() != Role.LEADER) throw new IllegalArgumentException("Leader slot requires a LEADER");
        this.leader = f;
    }

    public void assignAssistant(Facilitator f) {
        requireMutable();
        if (f != null && f.getRole() != Role.ASSISTANT) throw new IllegalArgumentException("Assistant slot requires an ASSISTANT");
        this.assistant = f;
    }

    public void setLeader(Facilitator facilitator) { assignLeader(facilitator); }
    public void setAssistant(Facilitator facilitator) { assignAssistant(facilitator); }
    public int getSize() { return participants.size(); }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public Facilitator getLeader() {
        return leader;
    }

    public Facilitator getAssistant() {
        return assistant;
    }

    public Gender getGenderTarget() {
        return genderTarget;
    }

    public AgeTier getAgeTierTarget() {
        return ageTierTarget;
    }

    public LifeStage getLifeStageTarget() {
        return lifeStageTarget;
    }
}
