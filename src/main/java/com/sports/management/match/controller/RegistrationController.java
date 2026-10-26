package com.sports.management.match.controller;

import com.sports.management.match.model.Participant;
import com.sports.management.match.model.Registration;
import com.sports.management.match.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/registrations")
    public ResponseEntity<Registration> register(@Valid @RequestBody Registration registration) {
        Registration created = registrationService.register(registration);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<Registration>> getAllRegistrations() {
        return ResponseEntity.ok(registrationService.getAllRegistrations());
    }

    @GetMapping("/registrations/{id}")
    public ResponseEntity<Registration> getRegistrationById(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.getRegistrationById(id));
    }

    @GetMapping("/registrations/competition/{competitionId}")
    public ResponseEntity<List<Registration>> getRegistrationsByCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(registrationService.getRegistrationsByCompetition(competitionId));
    }

    @PutMapping("/registrations/{id}/approve")
    public ResponseEntity<Registration> approveRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.approveRegistration(id));
    }

    @PutMapping("/registrations/{id}/reject")
    public ResponseEntity<Registration> rejectRegistration(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = (body != null && body.containsKey("reason")) ? body.get("reason") : "Registration rejected by organizer";
        return ResponseEntity.ok(registrationService.rejectRegistration(id, reason));
    }

    @GetMapping("/participants/competition/{competitionId}")
    public ResponseEntity<List<Participant>> getParticipantsByCompetition(@PathVariable Long competitionId) {
        return ResponseEntity.ok(registrationService.getParticipantsByCompetition(competitionId));
    }
}
