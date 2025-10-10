package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
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
        // Client 초기화: Client 빌더에서 apiKey() 메서드를 사용
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
    public MatchRegistrationDto analyzeAndStructureData(String imageUrl, String winnerTeam, Long serverId) throws Exception {

        // 1. 이미지 URL에서 바이트 배열 다운로드
        byte[] imageBytes = downloadImageBytes(imageUrl);

        // 2. 프롬프트 및 시스템 설정 정의
        String prompt = "Please extract the stats for all 10 players from the League of Legends match result screen image. Extract 'gameName', 'tagLine', 'team' (RED/BLUE), 'kills', 'deaths', and 'assists'. If the team is not visible, infer it based on the image's layout (usually left is one team, right is the other). The user specified the winner team is " + winnerTeam + ".";

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

        // 6. 응답 파싱 및 안전한 Optional 처리 ⭐⭐⭐
        if (response == null || !response.candidates().isPresent() || response.candidates().get().isEmpty()) {
            throw new Exception("Gemini API에서 유효한 응답을 받지 못했습니다.");
        }

        // Optional 체이닝을 사용하여 안전하게 텍스트를 추출
        String rawResponseText = response.candidates().get().stream()
                .findFirst()
                .flatMap(candidate -> candidate.content())
                .flatMap(content -> content.parts())
                .flatMap(parts -> parts.stream().findFirst())
                .flatMap(part -> part.text())
                .orElseThrow(() -> new Exception("Gemini API 응답에서 JSON 문자열을 찾을 수 없습니다. (응답 구조 오류)"));

        // ⭐⭐⭐ 핵심 수정 부분 ⭐⭐⭐
        String jsonString = rawResponseText
                .replace("```json", "") // 마크다운 시작 백틱 + 언어 키워드 제거
                .replace("```", "")     // 마크다운 종료 백틱 제거
                .trim();                // 앞뒤 공백 및 줄바꿈 제거
        // ⭐⭐⭐ 수정 끝 ⭐⭐⭐

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
        // AI가 추출한 데이터는 그대로 반환합니다.
        // Discord ID는 AI가 알 수 없으므로, 이 값은 서비스 로직에서 유효성 검증 시 DB 매핑을 통해 채워져야 합니다.
        return extracted;
    }
}