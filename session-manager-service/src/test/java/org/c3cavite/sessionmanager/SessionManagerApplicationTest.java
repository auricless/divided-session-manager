package org.c3cavite.sessionmanager;

import org.c3cavite.core.domain.SessionConfig;
import org.c3cavite.core.service.GroupAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionManagerApplicationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private GroupAssignmentService assignments;

    @Test
    void bootsAnHttpServerAndWiresTheStandaloneCore() {
        assertTrue(port > 0);
        assertTrue(assignments.assign(SessionConfig.builder().capSize(6).build(), List.of(), List.of()).isFinal());
    }
}
