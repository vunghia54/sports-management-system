
package com.sports.management.match.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TournamentClient {

    private final RestClient restClient;

    public TournamentClient(
            @Value("${services.tournament.url:http://localhost:8080}")
            String tournamentServiceUrl) {

        // Tao RestClient truc tiep, khong can inject Builder
        this.restClient = RestClient.builder()
                .baseUrl(tournamentServiceUrl)
                .build();
    }

    public boolean tournamentExists(Long tournamentId) {

        if (tournamentId == null) {
            return false;
        }

        try {
            restClient.get()
                    .uri("/api/v1/tournaments/{id}", tournamentId)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            (request, response) -> {
                                throw new TournamentNotFoundException();
                            }
                    )
                    .toBodilessEntity();

            return true;

        } catch (TournamentNotFoundException ex) {
            return false;

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Tournament Service is temporarily unavailable",
                    ex
            );
        }
    }

    private static class TournamentNotFoundException
            extends RuntimeException {
    }
}
