package org.c3cavite.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group {
    private final List<Participant> participants = new ArrayList<>();
    private Facilitator leader;
    private Facilitator assistant;
    
    // Metadata for tracking/validation
    private final Gender genderTarget;
    private final AgeTier ageTierTarget;
    private final LifeStage lifeStageTarget;

    public Group(Gender genderTarget, AgeTier ageTierTarget, LifeStage lifeStageTarget) {
        this.genderTarget = genderTarget;
        this.ageTierTarget = ageTierTarget;
        this.lifeStageTarget = lifeStageTarget;
    }

    public void addParticipant(Participant p) {
        participants.add(p);
    }

    public void assignLeader(Facilitator f) {
        this.leader = f;
    }

    public void assignAssistant(Facilitator f) {
        this.assistant = f;
    }

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
