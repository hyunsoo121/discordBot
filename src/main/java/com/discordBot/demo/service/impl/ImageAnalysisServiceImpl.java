package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.Line;
import com.discordBot.demo.domain.repository.LineRepository;
import com.discordBot.demo.service.ChampionService;
import com.discordBot.demo.service.ImageAnalysisService;
import com.discordBot.demo.service.RiotApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
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
    private final ChampionService championService;
    private final RiotApiService riotApiService;
    private final LineRepository lineRepository;

    @Value("classpath:prompts/match_data_prompt.txt")
    private Resource matchDataPromptResource;

    @Value("classpath:prompts/system_instruction.txt")
    private Resource systemInstructionResource;

    private String matchDataPromptTemplate;
    private String systemInstruction;
    private List<Line> allLines;


    public ImageAnalysisServiceImpl(
            @Value("${spring.gemini.api.key}") String apiKey,
            ChampionService championService,
            RiotApiService riotApiService,
            LineRepository lineRepository
    ) {
        this.geminiClient = Client.builder().apiKey(apiKey).build();
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient();
        this.championService = championService;
        this.riotApiService = riotApiService;
        this.lineRepository = lineRepository;
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

            this.allLines = lineRepository.findAll();

            log.info("✅ 프롬프트 템플릿 및 라인 데이터 로드 완료.");
        } catch (IOException e) {
            log.error("❌ 리소스 파일 로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("프롬프트 파일 초기화 실패", e);
        }
    }

    // JSON 응답 구조 (PlayerStatsDto에 laneName 필드가 있다고 가정)
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

        // 1. 힌트 목록 생성
        String userHintList = registeredAccounts.stream()
                .map(LolAccount::getFullAccountName)
                .collect(Collectors.joining(", "));
        String championHintList = String.join(", ", championService.getAllChampionNamesForHint());

        // ⭐⭐ 2. 라인 추정을 위한 시각적 힌트 URL 획득 및 프롬프트 생성 ⭐⭐
        String smiteUrl = riotApiService.getSmiteIconUrl();
        List<String> supportItemUrls = riotApiService.getSupportItemIconUrls();

        // ⭐⭐ 로깅 추가: 아이템 URL이 유효한지 확인
        log.info("Riot HINT URLS: Smite={} | Support Items Count={}", smiteUrl, supportItemUrls.size());


        // 텍스트 프롬프트에 모든 힌트 URL을 포함시켜 Gemini에게 판단 근거를 제공
        String visualHintPrompt = String.format(
                "라인(laneName) 추정을 위해 다음 규칙을 적용하세요: " +
                        "1. 스펠 슬롯에서 강타 아이콘(%s)이 보이면 'JUNGLE'로 설정하세요. " +
                        "2. 시작 아이템 슬롯에서 서포터 아이템(%s) 중 하나가 보이면 'SUPPORT'로 설정하세요. " +
                        "3. 두 가지 조건이 모두 해당되지 않는다면, 'UNKNOWN'으로 두세요. " +
                        "4. 라인 코드는 TOP, JUNGLE, MIDDLE, BOTTOM, SUPPORT 중 하나여야 합니다. " +
                        "추가 규칙: **각 팀(BLUE, RED) 내에서 TOP, JUNGLE, MIDDLE, BOTTOM, SUPPORT 라인은 오직 하나씩만 존재해야 합니다.**", // ⭐ 구조적 규칙 추가
                smiteUrl, String.join(", ", supportItemUrls)
        );

        // 3. 최종 프롬프트 결합
        String structuralRule = "⭐⭐중요 규칙: 각 팀은 5개의 라인(TOP, JUNGLE, MIDDLE, BOTTOM, SUPPORT)을 정확히 하나씩 가져야 합니다. 추정 시 이 규칙을 최우선으로 적용하세요.⭐⭐";

        String combinedPrompt = String.format(matchDataPromptTemplate, userHintList, championHintList) +
                "\n\n" + visualHintPrompt +
                "\n\n" + structuralRule; // ⭐ 구조적 규칙을 더욱 강조

        // Gemini API 호출
        GenerateContentResponse response = callGeminiApi(combinedPrompt, imageBytes);
        String rawJsonString = extractRawJsonText(response);
        JsonExtractionResult extractionResult = parseAndValidateJson(rawJsonString);

        // ⭐⭐ 4. 라인 추정 후처리 (Gemini가 UNKNOWN으로 반환한 경우) ⭐⭐
        for (PlayerStatsDto playerDto : extractionResult.players) {

            String geminiLane = playerDto.getLaneName();

            // Gemini가 라인을 추정하지 못했거나, 지원하지 않는 라인 코드를 반환한 경우
            if (!StringUtils.hasText(geminiLane) || geminiLane.equalsIgnoreCase("UNKNOWN") || !isValidLine(geminiLane)) {

                // 등록된 LolAccount를 찾아 선호 라인 정보를 확인
                LolAccount account = registeredAccounts.stream()
                        .filter(a -> a.getGameName().equalsIgnoreCase(playerDto.getLolGameName()))
                        .findFirst()
                        .orElse(null);

                if (account != null && account.getPreferredLines() != null && !account.getPreferredLines().isEmpty()) {
                    // 선호 라인 중 첫 번째 라인을 라인으로 설정
                    String assumedLane = account.getPreferredLines().iterator().next().getName();
                    playerDto.setLaneName(assumedLane);
                } else {
                    // 최종적으로도 추정 불가
                    playerDto.setLaneName("UNKNOWN");
                }
            }

            // DB에 저장될 라인 이름이 Line 엔티티의 NAME 필드와 일치하도록 대문자화 (UNKNOWN은 그대로 유지)
            if (!playerDto.getLaneName().equals("UNKNOWN")) {
                playerDto.setLaneName(playerDto.getLaneName().toUpperCase());
            }
        }

        // 5. 최종 DTO 조립 및 반환
        return buildFinalMatchDto(extractionResult, serverId);
    }

    /**
     * 라인 코드가 DB에 정의된 유효한 라인인지 확인합니다.
     */
    private boolean isValidLine(String laneName) {
        if (!StringUtils.hasText(laneName)) return false;
        String upperCaseLane = laneName.toUpperCase();

        return allLines.stream().anyMatch(line -> line.getName().equals(upperCaseLane)); // ⭐ Line 엔티티 구조에 맞게 .getName() 추가
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
        // JSON 파싱
        JsonExtractionResult extractionResult = objectMapper.readValue(jsonString, JsonExtractionResult.class);

        // 1. 승패 텍스트 누락 검증 (재캡처 요청 로직)
        if ("FAILURE_NO_VICTORY_TEXT".equals(extractionResult.analysisStatus)) {
            throw new IllegalArgumentException(
                    "❌ 오류: 경기 결과 이미지에서 승패 여부를 확인할 수 없습니다. '승리' 또는 '패배' 문구가 보이도록 다시 캡처해주세요."
            );
        }

        // 2. 필수 데이터 누락 검증 (승리팀, 시간)
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

        finalDto.setPlayerStatsList(extractionResult.players.stream()
                .map(this::mapToPlayerStatsDto)
                .collect(Collectors.toList()));

        return finalDto;
    }

    private PlayerStatsDto mapToPlayerStatsDto(PlayerStatsDto extracted) {
        return extracted;
    }
}