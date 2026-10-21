package org.c3cavite.sessionmanager.api;

import org.c3cavite.core.service.GroupAssignmentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {
    private final GroupAssignmentService assignments;
    private final SessionAssignmentMapper mapper;

    SessionController(GroupAssignmentService assignments, SessionAssignmentMapper mapper) {
        this.assignments = assignments;
        this.mapper = mapper;
    }

    @PostMapping("/assign")
    public AssignmentResponse assign(@RequestBody AssignmentRequest request) {
        SessionAssignmentMapper.AssignmentInput input = mapper.toDomain(request);
        return mapper.toResponse(assignments.assign(input.config(), input.participants(), input.facilitators(), input.exceptions()));
    }
}
