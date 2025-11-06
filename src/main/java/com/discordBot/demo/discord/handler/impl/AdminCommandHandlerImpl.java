package com.discordBot.demo.discord.handler.impl;

import com.discordBot.demo.discord.handler.AdminCommandHandler;
import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.GuildServerRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.service.MatchRecordService;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminCommandHandlerImpl implements AdminCommandHandler {

    private final UserRepository userRepository;
    private final GuildServerRepository guildServerRepository;
    private final UserService userService;
    private final MatchRecordService matchRecordService;

    /**
     * '/init-data' 명령어를 처리하며, 현재 서버에 테스트 데이터를 주입합니다.
     */
    @Transactional
    public void handleInitDataCommand(SlashCommandInteractionEvent event) {

        event.deferReply(true).queue();


        Member member = event.getMember();
        if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
            event.getHook().sendMessage("❌ 오류: **데이터 초기화** 명령어는 서버 관리자만 사용할 수 있습니다.").queue();
            return;
        }

        Long discordServerId = event.getGuild().getIdLong();

        // 데이터 초기화는 시간이 걸리므로 deferReply는 SlashCommandListener에서 처리됩니다.

        try {
            // 이 서버 ID를 기반으로 데이터 초기화 로직 실행
            initData(discordServerId);
            event.getHook().sendMessage("✅ **[초기 데이터 주입 성공]** 5경기 데이터가 서버 " + event.getGuild().getName() + "에 성공적으로 등록되었습니다. 랭킹을 확인해 보세요.").queue();
        } catch (Exception e) {
            log.error("데이터 초기화 중 치명적 오류 발생 (서버 ID: {}): {}", discordServerId, e.getMessage(), e);
            // 사용자에게 오류 메시지를 친절하게 전달
            String userError = e.getMessage().contains("Riot Games") ? e.getMessage() : "데이터 처리 중 예기치 않은 오류가 발생했습니다. (자세한 내용은 로그 확인)";
            event.getHook().sendMessage("❌ **[데이터 주입 실패]** " + userError).queue();

            // RuntimeException 발생 시 @Transactional에 의해 롤백됩니다.
        }
    }

    // --- 2. Data Initializer Logic (Core) ---

    /**
     * DataInitializer의 핵심 로직을 그대로 가져와 트랜잭션 내부에서 실행합니다.
     */
    private void initData(Long currentServerId) {
        log.info("⭐ [TEST DATA] 서버 ID {} 에 5경기 데이터 주입 시작...", currentServerId);

        // --- 1. 기본 엔티티 생성 ---
        // 명령어 실행 서버 ID를 사용
        GuildServer guildA = createGuildServer(currentServerId, "Target Server");

        User userFaker = createUser(1001L, "Faker_Fan");
        User userGuma = createUser(1002L, "Guma_Best");
        User userOner = createUser(1003L, "Oner_Jgl");
        User userKeria = createUser(1004L, "Keria_Sup");
        User userZeus = createUser(1005L, "Zeus_Top");

        // 롤 닉네임 등록 (유효한 Riot ID로 수정)
        try {
            // ⭐ Test 전용 메서드를 사용하거나, 유효한 계정을 사용해야 합니다.
            // 현재는 유효한 계정을 사용한다고 가정하고 registerLolNickname 호출
            userService.registerLolNickname(userFaker.getDiscordUserId(), "HEKSIS", "KR1", currentServerId, "TOP");
            userService.registerLolNickname(userGuma.getDiscordUserId(), "HEKSISE", "KR1", currentServerId, "JUNGLE");
            userService.registerLolNickname(userOner.getDiscordUserId(), "SISKEH", "KR1", currentServerId, "MID");
            userService.registerLolNickname(userKeria.getDiscordUserId(), "게자리", "KR1", currentServerId, "BOT");
            userService.registerLolNickname(userZeus.getDiscordUserId(), "블루애나", "KR1", currentServerId, "SUP");

            userRepository.flush();

        } catch (Exception e) {
            // 롤 계정 등록 실패는 치명적이므로 다시 던져서 롤백 유도
            throw new RuntimeException("User Registration Failed during Init: " + e.getMessage(), e);
        }

        // --- 2. 5개의 더미 경기 기록 생성 ---

        // Match 1: Faker팀 승리
        List<PlayerStatsDto> match1Stats = List.of(
                createPlayerStatsDto("HEKSISE", "KR1", "BLUE", 5, 2, 0),
                createPlayerStatsDto("HEKSIS", "KR1", "BLUE", 3, 4, 3),
                createPlayerStatsDto("SISKEH", "KR1", "RED", 10, 1, 0),
                createPlayerStatsDto("블루애나", "KR1", "RED", 0, 5, 5)
        );
        registerDummyMatch(currentServerId, "BLUE", match1Stats);

        // Match 2: Oner팀 승리 (KDA: Faker=5.0, Oner=20.0, Guma=1.0) - 예시 데이터와 닉네임 불일치 방지 수정
        List<PlayerStatsDto> match2Stats = List.of(
                createPlayerStatsDto("HEKSIS", "KR1", "RED", 10, 2, 0),   // K:10 D:2 A:0
                createPlayerStatsDto("SISKEH", "KR1", "RED", 15, 1, 5),    // K:15 D:1 A:5
                createPlayerStatsDto("블루애나", "KR1", "BLUE", 3, 3, 0),    // K:3 D:3 A:0
                createPlayerStatsDto("HEKSISE", "KR1", "BLUE", 1, 7, 6) // K:1 D:7 A:6
        );
        registerDummyMatch(currentServerId, "RED", match2Stats);

        // Match 3: Keria팀 승리
        List<PlayerStatsDto> match3Stats = List.of(
                createPlayerStatsDto("게자리", "KR1", "BLUE", 5, 1, 1),    // K:5 D:1 A:1
                createPlayerStatsDto("HEKSISE", "KR1", "BLUE", 4, 3, 2), // K:4 D:3 A:2
                createPlayerStatsDto("HEKSIS", "KR1", "RED", 2, 5, 1),    // K:2 D:5 A:1
                createPlayerStatsDto("SISKEH", "KR1", "RED", 1, 5, 0)       // K:1 D:5 A:0
        );
        registerDummyMatch(currentServerId, "BLUE", match3Stats);

        // Match 4: Guma팀 승리 (퍼펙트 KDA 테스트)
        List<PlayerStatsDto> match4Stats = List.of(
                createPlayerStatsDto("HEKSISE", "KR1", "RED", 15, 0, 0),     // K:15 D:0 A:0
                createPlayerStatsDto("게자리", "KR1", "RED", 4, 2, 0),    // K:4 D:2 A:0
                createPlayerStatsDto("HEKSIS", "KR1", "BLUE", 1, 8, 3), // K:1 D:8 A:3
                createPlayerStatsDto("블루애나", "KR1", "BLUE", 0, 4, 0)      // K:0 D:4 A:0
        );
        registerDummyMatch(currentServerId, "RED", match4Stats);

        // Match 5: Zeus팀 승리
        List<PlayerStatsDto> match5Stats = List.of(
                createPlayerStatsDto("블루애나", "KR1", "BLUE", 12, 1, 15), // K:12 D:1 A:15
                createPlayerStatsDto("게자리", "KR1", "BLUE", 6, 2, 10), // K:6 D:2 A:10
                createPlayerStatsDto("HEKSIS", "KR1", "RED", 3, 5, 5),    // K:3 D:5 A:5
                createPlayerStatsDto("SISKEH", "KR1", "RED", 4, 6, 8)       // K:4 D:6 A:8
        );
        registerDummyMatch(currentServerId, "BLUE", match5Stats);


        log.info("✅ [TEST DATA] 서버 ID {} 에 5경기 데이터 주입 완료.", currentServerId);
    }

    // --- 3. 헬퍼 메서드 (유지) ---

    private GuildServer createGuildServer(Long id, String name) {
        // findOrCreate 로직을 사용하거나, 단순 저장을 사용해야 합니다.
        GuildServer server = new GuildServer();
        server.setDiscordServerId(id);
        server.setServerName(name);
        return guildServerRepository.save(server);
    }

    private User createUser(Long id, String name) {
        User user = new User();
        user.setDiscordUserId(id);
        return userRepository.save(user);
    }

    private PlayerStatsDto createPlayerStatsDto(String gameName, String tagLine, String team, int kills, int deaths, int assists) {
        PlayerStatsDto dto = new PlayerStatsDto();
        dto.setLolGameName(gameName);
        dto.setLolTagLine(tagLine);
        dto.setTeam(team);
        dto.setKills(kills);
        dto.setDeaths(deaths);
        dto.setAssists(assists);
        return dto;
    }

    private void registerDummyMatch(Long serverId, String winnerTeam, List<PlayerStatsDto> stats) {
        MatchRegistrationDto matchDto = new MatchRegistrationDto();
        matchDto.setServerId(serverId);
        matchDto.setWinnerTeam(winnerTeam);
        matchDto.setPlayerStatsList(stats);

        try {
            matchRecordService.registerMatch(matchDto);
        } catch (Exception e) {
            throw new RuntimeException("Match Registration Failed for Server " + serverId + ": " + e.getMessage(), e);
        }
    }
}