package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.Champion;
import com.discordBot.demo.service.RiotApiService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.stream.Collectors; // Collectors import

@Service
@Slf4j
public class RiotApiServiceImpl implements RiotApiService {

    private WebClient webClient;
    private WebClient dataDragonWebClient;

    @Value("${spring.riot.api.key}")
    private String apiKey;

    private static final String RIOT_API_BASE_URL = "https://asia.api.riotgames.com";
    private static final String DDRAGON_BASE_URL = "https://ddragon.leagueoflegends.com";
    private static final String ACCOUNT_BY_RIOT_ID_PATH = "/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}";

    // ⭐ Data Dragon 상수 추가
    private static final String SMITE_SPELL_KEY = "SummonerSmite";
    private static final List<String> SUPPORT_ITEM_IDS = List.of(
            "3865", "3866", "3867", "3869", "3870", "3871", "3876", "3877"
    );
    // URL 형식: base/cdn/version/img/type/file.png
    private static final String ICON_PATH_FORMAT = "/cdn/%s/img/%s/%s.png";


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class DataDragonResponse {
        public Map<String, Champion> data;
    }

    public RiotApiServiceImpl() {
        this.webClient = WebClient.builder()
                .baseUrl(RIOT_API_BASE_URL)
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .build();

        this.dataDragonWebClient = WebClient.builder()
                .baseUrl(DDRAGON_BASE_URL)
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

            String championUrl = String.format("/cdn/%s/data/ko_KR/champion.json", latestVersion);

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

    // ⭐ --------------------------------------------------------------------------------
    // 3. Line/Position 추정용 Data Dragon URL 제공
    // ⭐ --------------------------------------------------------------------------------

    @Override
    public String getSmiteIconUrl() {
        String version = getLatestGameVersion();
        if (version == null) return null;

        // URL 형식: base/cdn/version/img/spell/SummonerSmite.png
        return String.format(DDRAGON_BASE_URL + ICON_PATH_FORMAT, version, "spell", SMITE_SPELL_KEY);
    }

    @Override
    public List<String> getSupportItemIconUrls() {
        String version = getLatestGameVersion();
        if (version == null) return Collections.emptyList();

        // 서폿 아이템 URL 목록 생성: base/cdn/version/img/item/itemId.png
        return SUPPORT_ITEM_IDS.stream()
                .map(itemId -> String.format(DDRAGON_BASE_URL + ICON_PATH_FORMAT, version, "item", itemId))
                .collect(Collectors.toList());
    }
}