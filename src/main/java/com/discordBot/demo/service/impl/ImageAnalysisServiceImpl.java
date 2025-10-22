package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.LolAccount;
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
        public String analysisStatus; // SUCCESS, FAILURE_NO_VICTORY_TEXT 등
        public String finalWinnerTeam; // "BLUE" 또는 "RED"
        public String team1Side; // 1팀의 실제 진영 ("BLUE" 또는 "RED")
        public int gameDurationSeconds; // 경기 지속 시간 (초)
        public List<PlayerStatsDto> players;
    }

    @Override
    public MatchRegistrationDto analyzeAndStructureData(String imageUrl, Long serverId, List<LolAccount> registeredAccounts) throws Exception {

        byte[] imageBytes = downloadImageBytes(imageUrl);

        String hintList = registeredAccounts.stream()
                .map(LolAccount::getFullAccountName)
                .collect(Collectors.joining(", "));

        // 프롬프트: 승패, 시간, 골드, 피해량 위치를 명시적으로 지정
        String prompt = String.format(
                "This is a League of Legends match result screen. Analyze the image to determine the winner (BLUE/RED), the game duration, and all player stats." +
                        "1. **Winning Side:** Check 'Victory'/'Defeat' text and background colors. If text is missing, set analysisStatus to FAILURE_NO_VICTORY_TEXT. Otherwise, set to SUCCESS and determine finalWinnerTeam (BLUE or RED) and team1Side (BLUE or RED)." +
                        "2. **Duration:** Find the **Total Game Duration** (format Xm Ys) and convert it to total seconds (e.g., 25m 30s -> 1530)." +
                        "3. **Stats:** Extract stats for all 10 players. Note the positions: 'totalDamage' is the second column from the right (often marked with a star *). 'totalGold' is the final column on the far right. Extract 'gameName', 'tagLine', 'team' (RED/BLUE), 'kills', 'deaths', 'assists', **'totalGold' (far right column)**, and **'totalDamage' (second from right)**. " +
                        "**Specific OCR Correction**: If you detect any confusion between the letter 'O' and the number '0', or between the letter 'I' and '1', always assume the letter variant unless the surrounding context is strictly numerical. Correct all player names accordingly." +
                        "**IMPORTANT**: Use the registered accounts list [%s] to correct any OCR errors and accurately map the Riot IDs.",
                hintList
        );

        // System Instruction: 반환해야 할 JSON 구조를 명시
        String systemInstructionString =
                "You are an expert esports match data extraction AI. Your response must be in strict, valid JSON format. The structure is: {\"analysisStatus\":\"string (SUCCESS or FAILURE_NO_VICTORY_TEXT)\", \"finalWinnerTeam\":\"string (BLUE or RED)\", \"team1Side\":\"string (BLUE or RED)\", \"gameDurationSeconds\":\"integer\", \"players\": [{\"gameName\":\"string\", \"tagLine\":\"string (e.g., KR1)\", \"team\":\"string (RED or BLUE)\", \"kills\":\"integer\", \"deaths\":\"integer\", \"assists\":\"integer\", \"totalGold\":\"integer\", \"totalDamage\":\"integer\"}, ...]}. Do not include any introductory or explanatory text outside the JSON block. Ensure 10 players are returned. If analysisStatus is FAILURE, finalWinnerTeam and gameDurationSeconds should be null or omitted.";

        // 3. Content 객체 생성 및 API 호출
        List<Content> contents = List.of(
                Content.builder()
                        .parts(List.of(
                                Part.fromText(prompt),
                                Part.fromBytes(imageBytes, "image/jpeg")
                        ))
                        .build()
        );

        Content systemInstructionContent = Content.builder()
                .parts(List.of(Part.fromText(systemInstructionString)))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstructionContent)
                .build();

        GenerateContentResponse response = geminiClient.models
                .generateContent(modelName, contents, config);

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

        // ⭐⭐ 1. 원본 JSON 응답 로그 출력 (디버깅용) ⭐⭐
        log.info("--- Gemini RAW JSON Start ---");
        log.info("{}", rawResponseText);
        log.info("--- Gemini RAW JSON End ---");


        String jsonString = rawResponseText
                .replace("```json", "")
                .replace("```", "")
                .trim();

        JsonExtractionResult extractionResult = objectMapper.readValue(jsonString, JsonExtractionResult.class);

        if ("FAILURE_NO_VICTORY_TEXT".equals(extractionResult.analysisStatus)) {
            throw new IllegalArgumentException(
                    "❌ 오류: 경기 결과 이미지에서 승패 여부를 확인할 수 없습니다. '승리' 또는 '패배' 문구가 보이도록 다시 캡처해주세요."
            );
        }

        String finalWinnerTeam = extractionResult.finalWinnerTeam;
        if (finalWinnerTeam == null || (!finalWinnerTeam.equals("BLUE") && !finalWinnerTeam.equals("RED")) ||
                extractionResult.gameDurationSeconds <= 0)
        {
            throw new IllegalArgumentException(
                    "❌ 오류: 필수 데이터(승리팀, 경기 시간) 추출에 실패했습니다. 이미지 분석 상태: " + extractionResult.analysisStatus
            );
        }

        int blueTotalGold = extractionResult.players.stream()
                .filter(p -> "BLUE".equalsIgnoreCase(p.getTeam()))
                .mapToInt(PlayerStatsDto::getTotalGold)
                .sum();

        int redTotalGold = extractionResult.players.stream()
                .filter(p -> "RED".equalsIgnoreCase(p.getTeam()))
                .mapToInt(PlayerStatsDto::getTotalGold)
                .sum();

        MatchRegistrationDto finalDto = new MatchRegistrationDto();
        finalDto.setServerId(serverId);
        finalDto.setWinnerTeam(finalWinnerTeam);
        finalDto.setTeam1Side(extractionResult.team1Side);

        finalDto.setGameDurationSeconds(extractionResult.gameDurationSeconds);
        finalDto.setBlueTotalGold(blueTotalGold);
        finalDto.setRedTotalGold(redTotalGold);

        finalDto.setPlayerStatsList(extractionResult.players.stream()
                .map(p -> mapToPlayerStatsDto(p, finalWinnerTeam))
                .collect(Collectors.toList()));

        return finalDto;
    }

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