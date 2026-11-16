package com.sports.management.athlete.service;

import com.sports.management.athlete.model.Athlete;
import com.sports.management.athlete.model.AthleteResponse;
import com.sports.management.athlete.repository.AthleteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AthleteService {

    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "fullName", "dateOfBirth", "gender", "nationality", "healthStatus"
    );

    public static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");

    public static final Set<String> ALLOWED_GENDERS = Set.of("MALE", "FEMALE", "OTHER");

    public static final Set<String> ALLOWED_HEALTH_STATUSES = Set.of("QUALIFIED", "INJURED", "SUSPENDED");

    public record AthletePage(
            List<AthleteResponse> content,
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

    public boolean canViewFullPii(Athlete athlete, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            return false;
        }
        boolean privileged = hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_ORGANIZER");
        if (privileged) {
            return true;
        }
        return athlete != null
                && athlete.getOwnerUsername() != null
                && !athlete.getOwnerUsername().isBlank()
                && athlete.getOwnerUsername().equalsIgnoreCase(authentication.getName());
    }

    public AthleteResponse toProjectedResponse(Athlete athlete, Authentication authentication) {
        return AthleteResponse.from(athlete, canViewFullPii(athlete, authentication));
    }

    public List<AthleteResponse> filterAthleteResponses(
            String name, String keyword, String gender, String healthStatus,
            String sortBy, String direction, Authentication authentication) {
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sortBy không hợp lệ: '" + sortBy + "'. Cho phép: " + ALLOWED_SORT_FIELDS);
        }
        String resolvedDirection = (direction == null || direction.isBlank()) ? "asc" : direction.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_DIRECTIONS.contains(resolvedDirection)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "direction không hợp lệ: '" + direction + "'. Cho phép: asc, desc");
        }

        Sort sort = "desc".equals(resolvedDirection)
                ? Sort.by(resolvedSortBy).descending()
                : Sort.by(resolvedSortBy).ascending();

        String kw = keyword != null && !keyword.isBlank() ? keyword.trim().toLowerCase() : null;

        return athleteRepository.findAll(sort).stream()
                .filter(a -> name == null || name.isBlank()
                        || (a.getFullName() != null && a.getFullName().toLowerCase().contains(name.toLowerCase())))
                .filter(a -> {
                    if (kw == null) return true;
                    if (a.getFullName() != null && a.getFullName().toLowerCase().contains(kw)) return true;
                    if (canViewFullPii(a, authentication)) {
                        return (a.getPhone() != null && a.getPhone().toLowerCase().contains(kw))
                                || (a.getEmail() != null && a.getEmail().toLowerCase().contains(kw))
                                || (a.getIdentityCardNumber() != null && a.getIdentityCardNumber().toLowerCase().contains(kw));
                    }
                    return false;
                })
                .filter(a -> gender == null || gender.isBlank()
                        || (a.getGender() != null && a.getGender().equalsIgnoreCase(gender)))
                .filter(a -> healthStatus == null || healthStatus.isBlank()
                        || (a.getHealthStatus() != null && a.getHealthStatus().equalsIgnoreCase(healthStatus)))
                .map(a -> toProjectedResponse(a, authentication))
                .toList();
    }

    public AthletePage searchAthletesPaged(
            String name, String keyword, String gender, String healthStatus,
            int page, int size, String sortBy, String direction, Authentication authentication) {
        List<AthleteResponse> filtered = filterAthleteResponses(
                name, keyword, gender, healthStatus, sortBy, direction, authentication);
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

    public AthleteResponse findByIdentityCardNumberAuthorized(String cccd, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Full authentication is required");
        }
        boolean privileged = hasAnyRole(authentication, "ROLE_ADMIN", "ROLE_ORGANIZER");
        boolean isTeamManager = hasAnyRole(authentication, "ROLE_TEAM_MANAGER");
        if (!privileged && !isTeamManager) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: Bạn không có quyền tra cứu VĐV theo số CCCD");
        }
        if (cccd == null || cccd.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số CCCD không được để trống");
        }
        Optional<Athlete> found = athleteRepository.findByIdentityCardNumber(cccd.trim());
        if (found.isEmpty()) {
            if (!privileged) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access Denied: Bạn không có quyền tra cứu VĐV theo số CCCD này");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Athlete not found with CCCD");
        }
        Athlete athlete = found.get();
        if (!canViewFullPii(athlete, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Access Denied: Bạn chỉ có thể tra cứu hồ sơ CCCD của VĐV thuộc quyền sở hữu của mình");
        }
        return toProjectedResponse(athlete, authentication);
    }

    @Transactional
    public Athlete createAthlete(Athlete athlete) {
        validateAndNormalizeAthlete(athlete, null, true);
        athlete.setId(null);
        return athleteRepository.save(athlete);
    }

    @Transactional
    public Athlete updateAthlete(Long id, Athlete updated) {
        Athlete existing = getAthleteById(id);
        if (existing == null) return null;
        validateAndNormalizeAthlete(updated, id, false);
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
        if (updates == null) return existing;

        if (updates.containsKey("fullName")) {
            String nextName = requireStringField(updates.get("fullName"), "fullName");
            if (nextName == null || nextName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Athlete fullName cannot be blank");
            }
            existing.setFullName(nextName.trim());
        }
        if (updates.containsKey("gender")) {
            existing.setGender(requireStringField(updates.get("gender"), "gender"));
        }
        if (updates.containsKey("identityCardNumber")) {
            existing.setIdentityCardNumber(requireStringField(updates.get("identityCardNumber"), "identityCardNumber"));
        }
        if (updates.containsKey("phone")) {
            existing.setPhone(requireStringField(updates.get("phone"), "phone"));
        }
        if (updates.containsKey("phoneNumber")) {
            existing.setPhone(requireStringField(updates.get("phoneNumber"), "phoneNumber"));
        }
        if (updates.containsKey("email")) {
            existing.setEmail(requireStringField(updates.get("email"), "email"));
        }
        if (updates.containsKey("healthStatus")) {
            existing.setHealthStatus(requireStringField(updates.get("healthStatus"), "healthStatus"));
        }
        if (updates.containsKey("nationality")) {
            existing.setNationality(requireStringField(updates.get("nationality"), "nationality"));
        }
        if (updates.containsKey("avatarUrl")) {
            existing.setAvatarUrl(requireStringField(updates.get("avatarUrl"), "avatarUrl"));
        }
        if (updates.containsKey("dateOfBirth")) {
            Object rawDob = updates.get("dateOfBirth");
            if (rawDob == null || String.valueOf(rawDob).isBlank()) {
                existing.setDateOfBirth(null);
            } else {
                try {
                    existing.setDateOfBirth(LocalDate.parse(String.valueOf(rawDob).trim()));
                } catch (DateTimeParseException ex) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "dateOfBirth không đúng định dạng yyyy-MM-dd: " + rawDob);
                }
            }
        }
        validateAndNormalizeAthlete(existing, id, false);
        return athleteRepository.save(existing);
    }

    private void validateAndNormalizeAthlete(Athlete athlete, Long excludeId, boolean defaultHealthIfBlank) {
        if (athlete.getFullName() == null || athlete.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Athlete full name is required");
        }
        athlete.setFullName(athlete.getFullName().trim());

        if (athlete.getDateOfBirth() != null && athlete.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày sinh không được lớn hơn ngày hiện tại (" + athlete.getDateOfBirth() + ")");
        }

        if (athlete.getGender() != null && !athlete.getGender().isBlank()) {
            String normalizedGender = athlete.getGender().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_GENDERS.contains(normalizedGender)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Giới tính không hợp lệ: '" + athlete.getGender() + "'. Cho phép: " + ALLOWED_GENDERS);
            }
            athlete.setGender(normalizedGender);
        } else {
            athlete.setGender(null);
        }

        if (athlete.getHealthStatus() == null || athlete.getHealthStatus().isBlank()) {
            if (defaultHealthIfBlank) {
                athlete.setHealthStatus("QUALIFIED");
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tình trạng thể lực không được để trống. Cho phép: " + ALLOWED_HEALTH_STATUSES);
            }
        } else {
            String normalizedHealth = athlete.getHealthStatus().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_HEALTH_STATUSES.contains(normalizedHealth)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tình trạng thể lực không hợp lệ: '" + athlete.getHealthStatus() + "'. Cho phép: " + ALLOWED_HEALTH_STATUSES);
            }
            athlete.setHealthStatus(normalizedHealth);
        }

        if (athlete.getIdentityCardNumber() != null && !athlete.getIdentityCardNumber().isBlank()) {
            String normalizedCccd = athlete.getIdentityCardNumber().trim();
            athlete.setIdentityCardNumber(normalizedCccd);
            Optional<Athlete> conflict = athleteRepository.findByIdentityCardNumber(normalizedCccd);
            if (conflict.isPresent() && (excludeId == null || !conflict.get().getId().equals(excludeId))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Số CCCD '" + normalizedCccd + "' đã tồn tại trong hệ thống.");
            }
        } else {
            athlete.setIdentityCardNumber(null);
        }
    }

    private String requireStringField(Object raw, String fieldName) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String s)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trường '" + fieldName + "' phải là chuỗi ký tự");
        }
        return s;
    }

    private boolean hasAnyRole(Authentication auth, String... roles) {
        if (auth == null) return false;
        for (String role : roles) {
            if (auth.getAuthorities().stream().anyMatch(a -> role.equalsIgnoreCase(a.getAuthority()))) {
                return true;
            }
        }
        return false;
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
