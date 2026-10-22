package org.c3cavite.sessionmanager.web;

import java.util.UUID;
import org.c3cavite.core.domain.AgeTier;
import org.c3cavite.core.domain.GenderMode;
import org.c3cavite.core.domain.SessionConfig;
import org.c3cavite.sessionmanager.api.AssignmentRequest;
import org.c3cavite.sessionmanager.api.SessionConfigMapper;
import org.c3cavite.sessionmanager.events.EventService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
class RegistrationPageController {
  private final EventService events;
  private final SessionConfigMapper configs;

  RegistrationPageController(EventService events, SessionConfigMapper configs) {
    this.events = events;
    this.configs = configs;
  }

  @GetMapping("/")
  String home() {
    return "redirect:/events";
  }

  @GetMapping("/events")
  String index(Model model) {
    var defaults = configs.toResponse(SessionConfig.builder().capSize(6).build());
    model.addAttribute("events", events.list());
    // Preserve core defaults but require the organizer to enter a cap for each new event.
    model.addAttribute(
        "config",
        new AssignmentRequest.Config(
            null,
            defaults.targetSize(),
            defaults.genderMode(),
            defaults.activeTiers(),
            defaults.lifeStageSubgroupingEnabled(),
            defaults.ageTierBoundaries()));
    choices(model);
    return "events";
  }

  @GetMapping("/events/{id}")
  String event(@PathVariable("id") UUID id, Model model) {
    var event = events.get(id);
    model.addAttribute("event", event);
    model.addAttribute("config", event.config());
    choices(model);
    return "registration";
  }

  @GetMapping("/events/{id}/groups")
  String groups(@PathVariable("id") UUID id, Model model) {
    model.addAttribute("event", events.get(id));
    return "groups";
  }

  private void choices(Model model) {
    model.addAttribute("tiers", AgeTier.values());
    model.addAttribute("modes", GenderMode.values());
  }
}
