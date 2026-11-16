package com.sports.management.athlete.service;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.repository.AthleteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AthleteService {

    public record AthletePage(
            List<Athlete> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    private final AthleteRepository athleteRepository;

    public AthleteService(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
    }

    public List<Athlete> getAllAthletes() {
        return athleteRepository.findAll();
    }

    public List<Athlete> filterAthletes(String name, String gender, String healthStatus, String sortBy, String direction) {
        return filterAthletes(name, null, gender, healthStatus, sortBy, direction);
    }

    public List<Athlete> filterAthletes(String name, String keyword, String gender, String healthStatus, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(sortBy != null ? sortBy : "id").descending()
                : Sort.by(sortBy != null ? sortBy : "id").ascending();

        String kw = keyword != null && !keyword.isBlank() ? keyword.trim().toLowerCase() : null;

        return athleteRepository.findAll(sort).stream()
                .filter(a -> name == null || name.isBlank()
                        || (a.getFullName() != null && a.getFullName().toLowerCase().contains(name.toLowerCase())))
                .filter(a -> kw == null
                        || (a.getFullName() != null && a.getFullName().toLowerCase().contains(kw))
                        || (a.getPhone() != null && a.getPhone().toLowerCase().contains(kw))
                        || (a.getEmail() != null && a.getEmail().toLowerCase().contains(kw))
                        || (a.getIdentityCardNumber() != null && a.getIdentityCardNumber().toLowerCase().contains(kw)))
                .filter(a -> gender == null || gender.isBlank()
                        || (a.getGender() != null && a.getGender().equalsIgnoreCase(gender)))
                .filter(a -> healthStatus == null || healthStatus.isBlank()
                        || (a.getHealthStatus() != null && a.getHealthStatus().equalsIgnoreCase(healthStatus)))
                .toList();
    }

    public AthletePage searchAthletesPaged(String name, String gender, String healthStatus, int page, int size, String sortBy, String direction) {
        return searchAthletesPaged(name, null, gender, healthStatus, page, size, sortBy, direction);
    }

    public AthletePage searchAthletesPaged(String name, String keyword, String gender, String healthStatus, int page, int size, String sortBy, String direction) {
        List<Athlete> filtered = filterAthletes(name, keyword, gender, healthStatus, sortBy, direction);
        int totalElements = filtered.size();
        int safeSize = Math.max(1, size);
        int safePage = Math.max(0, page);
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);
        int fromIndex = Math.min(safePage * safeSize, totalElements);
        int toIndex = Math.min(fromIndex + safeSize, totalElements);
        return new AthletePage(filtered.subList(fromIndex, toIndex), safePage, safeSize, totalElements, totalPages);
    }

    public Athlete getAthleteById(Long id) {
        return athleteRepository.findById(id).orElse(null);
    }

    @Transactional
    public Athlete createAthlete(Athlete athlete) {
        athlete.setId(null);
        return athleteRepository.save(athlete);
    }

    @Transactional
    public Athlete updateAthlete(Long id, Athlete updated) {
        Athlete existing = getAthleteById(id);
        if (existing == null) return null;
        existing.setFullName(updated.getFullName());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setGender(updated.getGender());
        existing.setIdentityCardNumber(updated.getIdentityCardNumber());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setNationality(updated.getNationality());
        existing.setAvatarUrl(updated.getAvatarUrl());
        existing.setHealthStatus(updated.getHealthStatus());
        if (updated.getOwnerUsername() != null) {
            existing.setOwnerUsername(updated.getOwnerUsername());
        }
        return athleteRepository.save(existing);
    }

    @Transactional
    public Athlete patchAthlete(Long id, Map<String, Object> updates) {
        Athlete existing = getAthleteById(id);
        if (existing == null) return null;
        if (updates.containsKey("fullName")) existing.setFullName((String) updates.get("fullName"));
        if (updates.containsKey("gender")) existing.setGender((String) updates.get("gender"));
        if (updates.containsKey("identityCardNumber")) existing.setIdentityCardNumber((String) updates.get("identityCardNumber"));
        if (updates.containsKey("phone")) existing.setPhone((String) updates.get("phone"));
        if (updates.containsKey("phoneNumber")) existing.setPhone((String) updates.get("phoneNumber"));
        if (updates.containsKey("email")) existing.setEmail((String) updates.get("email"));
        if (updates.containsKey("healthStatus")) existing.setHealthStatus((String) updates.get("healthStatus"));
        if (updates.containsKey("nationality")) existing.setNationality((String) updates.get("nationality"));
        if (updates.containsKey("dateOfBirth") && updates.get("dateOfBirth") != null) {
            existing.setDateOfBirth(LocalDate.parse(updates.get("dateOfBirth").toString()));
        }
        return athleteRepository.save(existing);
    }

    @Transactional
    public boolean deleteAthlete(Long id) {
        if (!athleteRepository.existsById(id)) return false;
        athleteRepository.deleteById(id);
        return true;
    }

    public Map<String, Object> getStatistics() {
        List<Athlete> all = athleteRepository.findAll();
        long male = all.stream().filter(a -> "MALE".equalsIgnoreCase(a.getGender())).count();
        long female = all.stream().filter(a -> "FEMALE".equalsIgnoreCase(a.getGender())).count();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("male", male);
        stats.put("female", female);
        stats.put("other", all.size() - male - female);
        return stats;
    }
}
