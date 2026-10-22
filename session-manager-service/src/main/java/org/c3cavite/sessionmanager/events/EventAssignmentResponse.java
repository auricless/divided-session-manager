package org.c3cavite.sessionmanager.events;

import java.util.UUID;
import org.c3cavite.sessionmanager.api.AssignmentResponse;

public record EventAssignmentResponse(UUID eventId, long revision, AssignmentResponse result) {}
