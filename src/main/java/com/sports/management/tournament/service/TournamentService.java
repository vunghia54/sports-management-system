
package com.sports.management.tournament.service;

import com.sports.management.tournament.model.Tournament;
import com.sports.management.tournament.repository.TournamentRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TournamentService {

    private final TournamentRepository repository;

    public TournamentService(TournamentRepository repository) {
        this.repository = repository;
    }

    public List<Tournament> getAllTournaments() {
        return repository.findAll();
    }

    public Tournament getTournamentById(Long id) {
        if (id == null) {
            return null;
        }
        return repository.findById(id).orElse(null);
    }

    public Tournament createTournament(Tournament tournament) {
        tournament.setId(null);
        return repository.save(tournament);
    }

    public Tournament updateTournament(
            Long id, Tournament updated) {

        Tournament tournament = getTournamentById(id);
        if (tournament == null) {
            return null;
        }

        tournament.setName(updated.getName());
        tournament.setDescription(updated.getDescription());
        tournament.setStartDate(updated.getStartDate());
        tournament.setEndDate(updated.getEndDate());
        tournament.setLocation(updated.getLocation());
        tournament.setRegistrationStartDate(
                updated.getRegistrationStartDate());
        tournament.setRegistrationEndDate(
                updated.getRegistrationEndDate());
        tournament.setStatus(updated.getStatus());

        return repository.save(tournament);
    }

    public boolean deleteTournament(Long id) {
        if (id == null || !repository.existsById(id)) {
            return false;
        }
        repository.deleteById(id);
        return true;
    }

    public Tournament patchTournament(
            Long id, Map<String, Object> updates) {

        Tournament tournament = getTournamentById(id);

        if (tournament == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tournament not found with id: " + id);
        }

        if (updates == null || updates.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "PATCH body cannot be empty");
        }

        Set<String> allowed = Set.of(
                "name", "description", "startDate", "endDate",
                "location", "registrationStartDate",
                "registrationEndDate", "status"
        );

        for (String field : updates.keySet()) {
            if (!allowed.contains(field)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Field cannot be updated: " + field);
            }
        }

        Tournament candidate = new Tournament(
                tournament.getId(),
                tournament.getName(),
                tournament.getDescription(),
                tournament.getStartDate(),
                tournament.getEndDate(),
                tournament.getLocation(),
                tournament.getRegistrationStartDate(),
                tournament.getRegistrationEndDate(),
                tournament.getStatus()
        );

        updates.forEach((field, value) -> {
            switch (field) {
                case "name" ->
                        candidate.setName(requiredText(field, value));
                case "description" ->
                        candidate.setDescription(optionalText(field, value));
                case "location" ->
                        candidate.setLocation(requiredText(field, value));
                case "status" ->
                        candidate.setStatus(requiredText(field, value));
                case "startDate" ->
                        candidate.setStartDate(requiredDate(field, value));
                case "endDate" ->
                        candidate.setEndDate(requiredDate(field, value));
                case "registrationStartDate" ->
                        candidate.setRegistrationStartDate(
                                requiredDate(field, value));
                case "registrationEndDate" ->
                        candidate.setRegistrationEndDate(
                                requiredDate(field, value));
                default -> throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unknown field: " + field);
            }
        });

        if (candidate.getStartDate().isAfter(candidate.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Start date must not be after end date");
        }

        if (candidate.getRegistrationStartDate()
                .isAfter(candidate.getRegistrationEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Registration start date must not be after end date");
        }

        if (candidate.getRegistrationEndDate()
                .isAfter(candidate.getStartDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Registration must end before tournament starts");
        }

        tournament.setName(candidate.getName());
        tournament.setDescription(candidate.getDescription());
        tournament.setStartDate(candidate.getStartDate());
        tournament.setEndDate(candidate.getEndDate());
        tournament.setLocation(candidate.getLocation());
        tournament.setRegistrationStartDate(
                candidate.getRegistrationStartDate());
        tournament.setRegistrationEndDate(
                candidate.getRegistrationEndDate());
        tournament.setStatus(candidate.getStatus());

        return repository.save(tournament);
    }

    private String requiredText(String field, Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be a non-empty string");
        }
        return text;
    }

    private String optionalText(String field, Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be a string");
        }
        return text;
    }

    private LocalDate requiredDate(String field, Object value) {
        if (!(value instanceof String text)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be a date in yyyy-MM-dd format");
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    field + " must be a valid date in yyyy-MM-dd format");
        }
    }

    public TournamentPage searchTournaments(
            String name, String status, int page, int size,
            String sortBy, String direction) {

        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be >= 0 and size between 1 and 100");
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Direction must be asc or desc");
        }

        Set<String> sortFields = Set.of(
                "id", "name", "status", "location",
                "startDate", "endDate"
        );

        if (!sortFields.contains(sortBy)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid sortBy field: " + sortBy);
        }

        Sort.Direction sortDirection =
                direction.equalsIgnoreCase("desc")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(sortDirection, sortBy));

        Page<Tournament> result = repository.findAll(
                (root, query, cb) -> {
                    var predicates = new java.util.ArrayList<
                            jakarta.persistence.criteria.Predicate>();

                    if (name != null && !name.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("name")),
                                "%" + name.trim().toLowerCase() + "%"));
                    }

                    if (status != null && !status.isBlank()) {
                        predicates.add(cb.equal(
                                cb.lower(root.get("status")),
                                status.trim().toLowerCase()));
                    }

                    return cb.and(predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]));
                },
                pageable
        );

        return new TournamentPage(
                result.getContent(),
                page,
                size,
                Math.toIntExact(result.getTotalElements()),
                result.getTotalPages()
        );
    }

    public record TournamentPage(
            List<Tournament> content,
            int page,
            int size,
            int totalElements,
            int totalPages
    ) {
    }
}