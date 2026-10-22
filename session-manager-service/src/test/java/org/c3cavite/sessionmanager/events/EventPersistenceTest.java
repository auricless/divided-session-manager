package org.c3cavite.sessionmanager.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.c3cavite.sessionmanager.SessionManagerApplication;
import org.c3cavite.sessionmanager.api.AssignmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class EventPersistenceTest {
  @Test
  void eventConfigurationSurvivesACompleteApplicationContextRestart() {
    EventResponse created;
    try (var first =
        new SpringApplicationBuilder(SessionManagerApplication.class)
            .web(WebApplicationType.NONE)
            .run()) {
      created =
          first
              .getBean(EventService.class)
              .create(
                  new EventRequest(
                      "Restart acceptance",
                      new AssignmentRequest.Config(8, 5, null, null, null, null)));
    }
    try (var second =
        new SpringApplicationBuilder(SessionManagerApplication.class)
            .web(WebApplicationType.NONE)
            .run()) {
      EventService events = second.getBean(EventService.class);
      try {
        assertEquals(created, events.get(created.id()));
      } finally {
        events.delete(created.id());
      }
    }
  }
}
