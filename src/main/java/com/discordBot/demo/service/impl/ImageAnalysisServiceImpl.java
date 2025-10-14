package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.LolAccount; // LolAccount 엔티티 임포트
import com.discordBot.demo.service.ImageAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;

// Google GenAI SDK v1.8.0 import
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentConfig;

// 이미지 다운로드를 위한 okhttp 라이브러리 import
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageAnalysisServiceImpl implements ImageAnalysisService {

    private final Client geminiClient;
    private final String modelName = "gemini-2.5-flash";

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public ImageAnalysisServiceImpl(@Value("${spring.gemini.api.key}") String apiKey) {
        this.geminiClient = Client.builder()
                .apiKey(apiKey)
                .build();

        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient();
    }

    private static class JsonExtractionResult {
        public List<PlayerStatsDto> players;
    }

    @Override
    public MatchRegistrationDto analyzeAndStructureData(String imageUrl, String winnerTeam, Long serverId, List<LolAccount> registeredAccounts) throws Exception {

        // 1. 이미지 URL에서 바이트 배열 다운로드
        byte[] imageBytes = downloadImageBytes(imageUrl);

        // ⭐⭐⭐ 2. 프롬프트에 OCR 힌트 목록 추가 ⭐⭐⭐
        String hintList = registeredAccounts.stream()
                .map(LolAccount::getFullAccountName) // "Faker#KR1" 형태의 전체 이름 리스트 생성
                .collect(Collectors.joining(", "));

        String prompt = String.format(
                "Please extract the stats for all 10 players from the League of Legends match result screen image. Extract 'gameName', 'tagLine', 'team' (RED/BLUE), 'kills', 'deaths', and 'assists'. If the team is not visible, infer it based on the image's layout. The user specified the winner team is %s. " +
                        "**IMPORTANT**: The players' Riot IDs in the image are highly likely to be one of the following registered accounts: [%s]. Use this list to correct any OCR errors and accurately report the gameName and tagLine.",
                winnerTeam, hintList
        );
        // ⭐⭐⭐ 힌트 추가 끝 ⭐⭐⭐


        String systemInstructionString = "You are an expert esports match data extraction AI. Your response must be in strict, valid JSON format matching the structure: {\"players\": [{\"gameName\":\"string\", \"tagLine\":\"string\", \"team\":\"string (RED or BLUE)\", \"kills\":\"integer\", \"deaths\":\"integer\", \"assists\":\"integer\"}, ...]}. Do not include any introductory or explanatory text outside the JSON block. Ensure 10 players are returned.";

        // 3. Content 객체 생성 (프롬프트 + 이미지)
        List<Content> contents = List.of(
                Content.builder()
                        .parts(List.of(
                                Part.fromText(prompt),
                                Part.fromBytes(imageBytes, "image/jpeg")
                        ))
                        .build()
        );

        // 4. Gemini API 호출 설정 (System Instruction 포함)
        Content systemInstructionContent = Content.builder()
                .parts(List.of(Part.fromText(systemInstructionString)))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstructionContent)
                .build();

        // 5. Gemini API 호출
        GenerateContentResponse response = geminiClient.models
                .generateContent(modelName, contents, config);

        // 6. 응답 파싱 및 안전한 Optional 처리
        if (response == null || !response.candidates().isPresent() || response.candidates().get().isEmpty()) {
            throw new Exception("Gemini API에서 유효한 응답을 받지 못했습니다.");
        }

        String rawResponseText = response.candidates().get().stream()
                .findFirst()
                .flatMap(candidate -> candidate.content())
                .flatMap(content -> content.parts())
                .flatMap(parts -> parts.stream().findFirst())
                .flatMap(part -> part.text())
                .orElseThrow(() -> new Exception("Gemini API 응답에서 JSON 문자열을 찾을 수 없습니다. (응답 구조 오류)"));

        String jsonString = rawResponseText
                .replace("```json", "")
                .replace("```", "")
                .trim();

        JsonExtractionResult extractionResult = objectMapper.readValue(jsonString, JsonExtractionResult.class);

        // 7. MatchRegistrationDto 조립
        MatchRegistrationDto finalDto = new MatchRegistrationDto();
        finalDto.setServerId(serverId);
        finalDto.setWinnerTeam(winnerTeam);
        finalDto.setPlayerStatsList(extractionResult.players.stream()
                .map(p -> mapToPlayerStatsDto(p, winnerTeam))
                .collect(Collectors.toList()));

        return finalDto;
    }

    /**
     * 이미지 URL에서 데이터를 다운로드하여 바이트 배열로 반환하는 헬퍼 메서드
     */
    private byte[] downloadImageBytes(String imageUrl) throws IOException {
        Request request = new Request.Builder().url(imageUrl).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Discord 이미지 다운로드 실패: " + response);
            }
            return response.body().bytes();
        }
    }

    private PlayerStatsDto mapToPlayerStatsDto(PlayerStatsDto extracted, String winnerTeam) {
        return extracted;
    }
}