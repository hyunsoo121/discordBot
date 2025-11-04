package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.Champion; // Champion 엔티티 임포트
import com.discordBot.demo.service.RiotApiService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class RiotApiServiceImpl implements RiotApiService {
    private WebClient webClient;
    private WebClient dataDragonWebClient; // Data Dragon용 WebClient 인스턴스 추가

    @Value("${riot.api.key}")
    private String apiKey;

    // Riot Account API Endpoints
    private static final String ACCOUNT_BY_RIOT_ID_PATH = "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}";

    // Data Dragon URLs
    private static final String DDRAGON_BASE_URL = "https://ddragon.leagueoflegends.com/cdn";

    // Data Dragon 응답 구조
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DataDragonResponse {
        public Map<String, Champion> data;
    }

    public RiotApiServiceImpl() {
        // WebClient 인스턴스를 직접 생성하여 필드에 할당합니다.
        // NOTE: 이 방식은 Spring의 중앙 빌더 설정을 따르지 않습니다.
        this.webClient = WebClient.builder()
                .baseUrl("https://asia.api.riotgames.com")
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        // Data Dragon API용 WebClient (키가 필요 없어 다른 baseUrl 사용)
        this.dataDragonWebClient = WebClient.builder()
                .baseUrl(DDRAGON_BASE_URL)
                .build();
    }


    // --------------------------------------------------------------------------------
    // 1. Riot Account API (기존 기능 유지)
    // --------------------------------------------------------------------------------

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

    @Override
    public String getLatestGameVersion() {
        try {
            List<String> versions = this.dataDragonWebClient.get()
                    .uri("/api/versions.json")
                    .retrieve()
                    .onStatus(status -> status.isError(), resp -> resp.createException().flatMap(Mono::error))
                    .bodyToMono(List.class)
                    .block();

            if (versions != null && !versions.isEmpty()) {
                return versions.get(0);
            }
            return null;
        } catch (Exception e) {
            log.error("최신 Riot Game 버전을 가져오는 데 실패했습니다.", e);
            return null;
        }
    }

    @Override
    public Map<String, Champion> getLatestChampionDataAsChampionEntity() {
        try {
            String latestVersion = getLatestGameVersion();

            if (latestVersion == null) {
                return Collections.emptyMap();
            }

            // 챔피언 데이터 URL 구성: /cdn/{version}/data/ko_KR/champion.json
            String championUrl = String.format("/%s/data/ko_KR/champion.json", latestVersion);

            DataDragonResponse response = this.dataDragonWebClient.get()
                    .uri(championUrl)
                    .retrieve()
                    .onStatus(status -> status.isError(), resp -> resp.createException().flatMap(Mono::error))
                    .bodyToMono(DataDragonResponse.class)
                    .block();

            if (response == null || response.data == null) {
                return Collections.emptyMap();
            }

            return response.data;

        } catch (WebClientResponseException e) {
            log.error("Data Dragon API 통신 오류: 상태={}", e.getRawStatusCode(), e);
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("챔피언 데이터 조회 중 예기치 않은 오류 발생", e);
            return Collections.emptyMap();
        }
    }
}