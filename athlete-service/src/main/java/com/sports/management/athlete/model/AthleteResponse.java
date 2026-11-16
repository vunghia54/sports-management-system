package com.sports.management.athlete.model;

import java.time.LocalDate;

public record AthleteResponse(
        Long id,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String identityCardNumber,
        String phone,
        String phoneNumber,
        String email,
        String nationality,
        String avatarUrl,
        String healthStatus,
        String ownerUsername
) {
    public static AthleteResponse from(Athlete athlete, boolean includePii) {
        if (athlete == null) {
            return null;
        }
        if (includePii) {
            return new AthleteResponse(
                    athlete.getId(),
                    athlete.getFullName(),
                    athlete.getDateOfBirth(),
                    athlete.getGender(),
                    athlete.getIdentityCardNumber(),
                    athlete.getPhone(),
                    athlete.getPhone(),
                    athlete.getEmail(),
                    athlete.getNationality(),
                    athlete.getAvatarUrl(),
                    athlete.getHealthStatus(),
                    athlete.getOwnerUsername()
            );
        }
        return new AthleteResponse(
                athlete.getId(),
                athlete.getFullName(),
                null,
                athlete.getGender(),
                null,
                null,
                null,
                null,
                athlete.getNationality(),
                athlete.getAvatarUrl(),
                athlete.getHealthStatus(),
                null
        );
    }
}
