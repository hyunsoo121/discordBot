package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.service.ImageAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // ⭐ JsonIgnoreProperties import

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentConfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageAnalysisServiceImpl implements ImageAnalysisService {

    private final Client geminiClient;
    private final String modelName = "gemini-2.5-flash";

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    @Value("classpath:prompts/match_data_prompt.txt")
    private Resource matchDataPromptResource;

    @Value("classpath:prompts/system_instruction.txt")
    private Resource systemInstructionResource;

    private String matchDataPromptTemplate;
    private String systemInstruction;


    public ImageAnalysisServiceImpl(@Value("${spring.gemini.api.key}") String apiKey) {
        this.geminiClient = Client.builder()
                .apiKey(apiKey)
                .build();

        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient();
    }

    @PostConstruct
    public void loadPromptTemplates() {
        try {
            this.matchDataPromptTemplate = StreamUtils.copyToString(
                    matchDataPromptResource.getInputStream(),
                    StandardCharsets.UTF_8
            );

            this.systemInstruction = StreamUtils.copyToString(
                    systemInstructionResource.getInputStream(),
                    StandardCharsets.UTF_8
            );

            log.info("✅ 프롬프트 템플릿 로드 완료.");
        } catch (IOException e) {
            log.error("❌ 리소스 파일 로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("프롬프트 파일 초기화 실패", e);

        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JsonExtractionResult {
        public String analysisStatus;
        public String finalWinnerTeam;
        public String team1Side;
        public int gameDurationSeconds;
        public List<PlayerStatsDto> players;
    }

    @Override
    public MatchRegistrationDto analyzeAndStructureData(String imageUrl, Long serverId, List<LolAccount> registeredAccounts) throws Exception {

        byte[] imageBytes = downloadImageBytes(imageUrl);

        String hintList = registeredAccounts.stream()
                .map(LolAccount::getFullAccountName)
                .collect(Collectors.joining(", "));

        String prompt = String.format(matchDataPromptTemplate, hintList);

        GenerateContentResponse response = callGeminiApi(prompt, imageBytes);

        String rawResponseText = extractRawJsonText(response);

        JsonExtractionResult extractionResult = parseAndValidateJson(rawResponseText);

        return buildFinalMatchDto(extractionResult, serverId);
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

    private GenerateContentResponse callGeminiApi(String prompt, byte[] imageBytes) {
        List<Content> contents = List.of(
                Content.builder()
                        .parts(List.of(
                                Part.fromText(prompt),
                                Part.fromBytes(imageBytes, "image/jpeg")
                        ))
                        .build()
        );

        Content systemInstructionContent = Content.builder()
                .parts(List.of(Part.fromText(systemInstruction)))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstructionContent)
                .build();

        return geminiClient.models.generateContent(modelName, contents, config);
    }

    private String extractRawJsonText(GenerateContentResponse response) throws Exception {
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

        log.info("--- Gemini RAW JSON Start ---");
        log.info("{}", rawResponseText);
        log.info("--- Gemini RAW JSON End ---");

        String jsonString = rawResponseText
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int jsonStartIndex = -1;
        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);
            if (c == '{' || c == '[') {
                jsonStartIndex = i;
                break;
            }
        }

        if (jsonStartIndex != -1) {
            jsonString = jsonString.substring(jsonStartIndex);
        } else {
            throw new Exception("Gemini 응답에서 유효한 JSON 시작 토큰('{', '[')을 찾을 수 없습니다.");
        }


        return jsonString;
    }

    private JsonExtractionResult parseAndValidateJson(String jsonString) throws IOException, IllegalArgumentException {
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
        return extractionResult;
    }

    private MatchRegistrationDto buildFinalMatchDto(JsonExtractionResult extractionResult, Long serverId) {

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
        finalDto.setWinnerTeam(extractionResult.finalWinnerTeam);
        finalDto.setTeam1Side(extractionResult.team1Side);

        finalDto.setGameDurationSeconds(extractionResult.gameDurationSeconds);
        finalDto.setBlueTotalGold(blueTotalGold);
        finalDto.setRedTotalGold(redTotalGold);

        String finalWinnerTeam = extractionResult.finalWinnerTeam;
        finalDto.setPlayerStatsList(extractionResult.players.stream()
                .map(p -> mapToPlayerStatsDto(p))
                .collect(Collectors.toList()));

        return finalDto;
    }


    private PlayerStatsDto mapToPlayerStatsDto(PlayerStatsDto extracted) {
        return extracted;
    }
}