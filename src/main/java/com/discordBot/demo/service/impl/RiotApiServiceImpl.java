package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.service.RiotApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Service
public class RiotApiServiceImpl implements RiotApiService {

    private final WebClient webClient;

    @Value("${spring.riot.api.key}")
    private String apiKey;

    private static final String ACCOUNT_BY_RIOT_ID_PATH =
            "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}";
    public RiotApiServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("https://asia.api.riotgames.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();
    }

    @Override
    public Optional<RiotAccountDto> verifyNickname(String gameName, String tagLine) {

        try {
            RiotAccountDto account = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(ACCOUNT_BY_RIOT_ID_PATH)
                            .build(gameName, tagLine)
                    )
                    .header("X-Riot-Token", apiKey)

                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {

                        if (response.statusCode() == HttpStatus.NOT_FOUND) {
                            return response.createException();
                        }
                        return response.createException();
                    })
                    .bodyToMono(RiotAccountDto.class)
                    .block();

            return Optional.ofNullable(account);

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw new RuntimeException("Riot API 통신 오류 발생 (상태: " + e.getRawStatusCode() + ")", e);
        }
    }
}