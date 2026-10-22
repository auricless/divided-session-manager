package org.c3cavite.sessionmanager.events;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.c3cavite.sessionmanager.api.AssignmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationPageTest {
  @LocalServerPort private int port;
  @Autowired private EventService events;

  @Test
  void rendersEventCreationAndEscapedRegistrationWithConfigurationControls() throws Exception {
    var index = get("/events");
    assertEquals(200, index.statusCode());
    assertTrue(index.body().contains("Create event"));
    assertTrue(index.body().contains("capSize"));
    var event =
        events.create(
            new EventRequest(
                "<script>alert('x')</script>",
                new AssignmentRequest.Config(6, null, null, null, null, null)));
    try {
      var page = get("/events/" + event.id());
      assertEquals(200, page.statusCode(), page.body());
      assertTrue(page.body().contains("Register participant"));
      assertTrue(page.body().contains("Facilitator roster"));
      assertTrue(page.body().contains("allowAgeTierMismatch"));
      assertTrue(page.body().contains("lifeStageSubgroupingEnabled"));
      assertFalse(page.body().contains("<script>alert('x')</script>"));
      assertTrue(page.body().contains("&lt;script&gt;"));
      assertTrue(
          page.headers()
              .firstValue("Content-Security-Policy")
              .orElseThrow()
              .contains("script-src 'self'"));
      assertEquals("no-store", page.headers().firstValue("Cache-Control").orElseThrow());
    } finally {
      events.delete(event.id());
    }
  }

  @Test
  void registrationAssetsAreAvailableAndUnknownEventsRemainNotFound() throws Exception {
    assertEquals(200, get("/assets/registration.js").statusCode());
    assertEquals(200, get("/assets/session-manager.css").statusCode());
    assertEquals(404, get("/events/00000000-0000-0000-0000-000000000000").statusCode());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
            HttpResponse.BodyHandlers.ofString());
  }
}
