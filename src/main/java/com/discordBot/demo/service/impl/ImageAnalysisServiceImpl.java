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

    // ⭐ 수정 1: JSON 구조 확장 - 상태 및 최종 승리 진영 포함
    private static class JsonExtractionResult {
        public String analysisStatus; // SUCCESS, FAILURE_NO_VICTORY_TEXT 등
        public String finalWinnerTeam; // "BLUE" 또는 "RED"
        public List<PlayerStatsDto> players;
    }

    @Override
    // winnerTeam 인자 제거
    public MatchRegistrationDto analyzeAndStructureData(String imageUrl, Long serverId, List<LolAccount> registeredAccounts) throws Exception {

        byte[] imageBytes = downloadImageBytes(imageUrl);

        String hintList = registeredAccounts.stream()
                .map(LolAccount::getFullAccountName)
                .collect(Collectors.joining(", "));

        // ⭐ 수정 2: 프롬프트 변경 - 상태 보고 및 승패 확정 로직 요청
        String prompt = String.format(
                "This is a League of Legends match result screen. Analyze the image to determine the winner based on two factors: 1) The presence of 'Victory' or 'Defeat' text. 2) The background color of Team 1 (Top) and Team 2 (Bottom) to determine their side (Blue/Red). If you cannot clearly read 'Victory' or 'Defeat' text, set analysisStatus to FAILURE_NO_VICTORY_TEXT. Otherwise, set it to SUCCESS and determine the final winning side (BLUE or RED)." +
                        "Extract 'gameName', 'tagLine', 'team' (RED/BLUE), 'kills', 'deaths', and 'assists' for all 10 players. " +
                        "**IMPORTANT**: Use the registered accounts list [%s] to correct any OCR errors and accurately map the Riot IDs.",
                hintList
        );

        // ⭐ 수정 3: System Instruction 변경 - analysisStatus 필드 추가 요구
        String systemInstructionString =
                "You are an expert esports match data extraction AI. Your response must be in strict, valid JSON format matching the structure: {\"analysisStatus\":\"string (SUCCESS or FAILURE_NO_VICTORY_TEXT)\", \"finalWinnerTeam\":\"string (BLUE or RED)\", \"players\": [{\"gameName\":\"string\", \"tagLine\":\"string (e.g., KR1)\", \"team\":\"string (RED or BLUE)\", \"kills\":\"integer\", \"deaths\":\"integer\", \"assists\":\"integer\"}, ...]}. Do not include any introductory or explanatory text outside the JSON block. Ensure 10 players are returned. If analysisStatus is FAILURE, finalWinnerTeam should be null or omitted.";

        // 3. Content 객체 생성 및 API 호출 (이하 로직 유지)
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

        String jsonString = rawResponseText
                .replace("```json", "")
                .replace("```", "")
                .trim();

        JsonExtractionResult extractionResult = objectMapper.readValue(jsonString, JsonExtractionResult.class);

        // ⭐ 수정 4: 핵심 검증 로직 추가 (승패 텍스트 누락 시 예외 발생)
        if ("FAILURE_NO_VICTORY_TEXT".equals(extractionResult.analysisStatus)) {
            throw new IllegalArgumentException(
                    "❌ 오류: 경기 결과 이미지에서 승패 여부를 확인할 수 없습니다. '승리' 또는 '패배' 문구가 보이도록 다시 캡처해주세요."
            );
        }

        String finalWinnerTeam = extractionResult.finalWinnerTeam;
        if (finalWinnerTeam == null || (!finalWinnerTeam.equals("BLUE") && !finalWinnerTeam.equals("RED"))) {
            throw new IllegalArgumentException(
                    "❌ 오류: 승리팀(BLUE/RED) 확정에 실패했습니다. 이미지 분석 상태: " + extractionResult.analysisStatus
            );
        }

        // 5. MatchRegistrationDto 조립
        MatchRegistrationDto finalDto = new MatchRegistrationDto();
        finalDto.setServerId(serverId);
        finalDto.setWinnerTeam(finalWinnerTeam);
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