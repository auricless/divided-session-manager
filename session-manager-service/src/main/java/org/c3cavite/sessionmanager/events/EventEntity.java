package org.c3cavite.sessionmanager.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity(name = "SessionEvent")
@Table(name = "session_events")
class EventEntity {
  @Id private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "config_json", nullable = false, columnDefinition = "text")
  private String configJson;

  @Version private Long revision;

  protected EventEntity() {}

  EventEntity(String name, String configJson) {
    this.id = UUID.randomUUID();
    update(name, configJson);
  }

  void update(String name, String configJson) {
    this.name = name;
    this.configJson = configJson;
  }

  UUID id() {
    return id;
  }

  String name() {
    return name;
  }

  String configJson() {
    return configJson;
  }

  long revision() {
    return revision == null ? 0 : revision;
  }
}
