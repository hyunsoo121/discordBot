//package com.discordBot.demo.config;
//
//import com.discordBot.demo.domain.dto.MatchRegistrationDto;
//import com.discordBot.demo.domain.dto.PlayerStatsDto;
//import com.discordBot.demo.domain.entity.GuildServer;
//import com.discordBot.demo.domain.entity.User;
//import com.discordBot.demo.domain.repository.GuildServerRepository;
//import com.discordBot.demo.domain.repository.UserRepository;
//import com.discordBot.demo.service.MatchRecordService;
//import com.discordBot.demo.service.UserService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.event.ContextRefreshedEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class DataInitializer {
//
//    private final UserRepository userRepository;
//    private final GuildServerRepository guildServerRepository;
//    private final UserService userService;
//    private final MatchRecordService matchRecordService;
//
//    private static final Long MAIN_SERVER_ID = 500L;
//    private boolean alreadySetup = false;
//
//    @EventListener(ContextRefreshedEvent.class)
//    @Transactional
//    public void initData() {
//        if (alreadySetup) return;
//        log.info("⭐ [TEST DATA] 5경기 기반 초기 테스트 데이터 주입 시작...");
//
//        // --- 1. 기본 엔티티 생성 ---
//        GuildServer guildA = createGuildServer(MAIN_SERVER_ID, "T1_내전_서버");
//
//        User userFaker = createUser(1001L, "Faker_Fan");
//        User userGuma = createUser(1002L, "Guma_Best");
//        User userOner = createUser(1003L, "Oner_Jgl");
//        User userKeria = createUser(1004L, "Keria_Sup");
//        User userZeus = createUser(1005L, "Zeus_Top");
//
//        // 롤 닉네임 등록 (서버별 계정 연결)
//        try {
//            userService.registerLolNickname(userFaker.getDiscordUserId(), "HEKSIS", "KR1", guildA.getDiscordServerId());
//            userService.registerLolNickname(userGuma.getDiscordUserId(), "HEKSISE", "KR1", guildA.getDiscordServerId());
//            userService.registerLolNickname(userOner.getDiscordUserId(), "SISKEH", "KR1", guildA.getDiscordServerId());
//            userService.registerLolNickname(userKeria.getDiscordUserId(), "게자리", "KR1", guildA.getDiscordServerId());
//            userService.registerLolNickname(userZeus.getDiscordUserId(), "블루애나", "KR1", guildA.getDiscordServerId());
//
//            // ⭐ 오류 해결: 롤 계정 등록 후 DB에 강제 반영 (플러시)
//            userRepository.flush();
//
//        } catch (Exception e) {
//            log.error("[TEST DATA] 롤 계정 등록 중 오류 발생: {}", e.getMessage());
//            throw new RuntimeException("InitData User Registration Failed", e);
//        }
//
//        // --- 2. 5개의 더미 경기 기록 생성 (통계 누적) ---
//
//        // (이하 5개 경기 기록 코드는 동일)
//        List<PlayerStatsDto> match1Stats = List.of(
//                createPlayerStatsDto("HEKSISE", "KR1", "BLUE", 5, 2, 0),
//                createPlayerStatsDto("HEKSIS", "KR1", "BLUE", 3, 4, 3),
//                createPlayerStatsDto("SISKEH", "KR1", "RED", 10, 1, 0),
//                createPlayerStatsDto("블루애나", "KR1", "RED", 0, 5, 5)
//        );
//        registerDummyMatch(MAIN_SERVER_ID, "BLUE", match1Stats);
//
//        List<PlayerStatsDto> match5Stats = List.of(
//                createPlayerStatsDto("HEKSISE", "KR1", "RED", 15, 0, 0),
//                createPlayerStatsDto("HEKSIS", "KR1", "RED", 4, 2, 0),
//                createPlayerStatsDto("SISKEH", "KR1", "BLUE", 1, 8, 3),
//                createPlayerStatsDto("블루애나", "KR1", "BLUE", 0, 4, 0)
//        );
//        registerDummyMatch(MAIN_SERVER_ID, "RED", match5Stats);
//
//
//        log.info("✅ [TEST DATA] 초기 테스트 데이터 주입 완료.");
//        alreadySetup = true;
//    }
//
//    // --- 헬퍼 메서드 (엔티티 생성 및 매치 등록) ---
//
//    private GuildServer createGuildServer(Long id, String name) {
//        GuildServer server = new GuildServer();
//        server.setDiscordServerId(id);
//        server.setServerName(name);
//        return guildServerRepository.save(server);
//    }
//
//    private User createUser(Long id, String name) {
//        User user = new User();
//        user.setDiscordUserId(id);
//        return userRepository.save(user);
//    }
//
//    private PlayerStatsDto createPlayerStatsDto(String gameName, String tagLine, String team, int kills, int deaths, int assists) {
//        PlayerStatsDto dto = new PlayerStatsDto();
//        dto.setLolGameName(gameName);
//        dto.setLolTagLine(tagLine);
//        dto.setTeam(team);
//        dto.setKills(kills);
//        dto.setDeaths(deaths);
//        dto.setAssists(assists);
//        return dto;
//    }
//
//    private void registerDummyMatch(Long serverId, String winnerTeam, List<PlayerStatsDto> stats) {
//        MatchRegistrationDto matchDto = new MatchRegistrationDto();
//        matchDto.setServerId(serverId);
//        matchDto.setWinnerTeam(winnerTeam);
//        matchDto.setPlayerStatsList(stats);
//
//        try {
//            matchRecordService.registerMatch(matchDto);
//        } catch (Exception e) {
//            // ⭐ 오류 로그를 출력하여 디버깅 정보 확보
//            log.error("경기 등록 실패! 원인: {}", e.getMessage());
//            throw new RuntimeException("InitData Match Registration Failed", e);
//        }
//    }
//}
