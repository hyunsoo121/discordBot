package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Service
public class RiotApiServiceImpl implements RiotApiService{

    @Value("${riot.api_key")
    private String apiKey;

    @Override
    public Optional<RiotAccountDto> verifyNickname(String gameName, String tagLine) {
        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" +
                gameName + "/" + tagLine + "?api_key=" + apiKey;

        try {
            // 2. HTTP 요청 및 응답 처리
            // RestTemplate, WebClient 또는 Discord.js 라이브러리 내장 기능 사용
            // 예시: WebClient 사용
            RiotAccountDto account = WebClient.create().get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(RiotAccountDto.class)
                    .block();

            return Optional.ofNullable(account);

        } catch (WebClientResponseException e) {
            // 404 (Not Found) 등의 오류 처리: 닉네임이 존재하지 않음
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            // 그 외 API 오류 처리 (Rate Limit 등)
            throw new RuntimeException("Riot API 오류 발생", e);
        }
    }
}
