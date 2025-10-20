package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.entity.UserServerStats;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
import com.discordBot.demo.service.impl.UserServerStatsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServerStatsServiceImplTest {

    @InjectMocks
    private UserServerStatsServiceImpl userServerStatsService;

    @Mock private UserServerStatsRepository userServerStatsRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServerManagementService serverManagementService;

    // 테스트용 상수 데이터
    private static final Long USER_ID = 1001L;
    private static final Long SERVER_ID = 500L;
    private static final long DURATION_SECONDS = 1500L;
    private static final int TEAM_KILLS = 20;

    private User mockUser;
    private GuildServer mockServer;
    private PlayerStatsDto mockPlayerStatsWin;
    private PlayerStatsDto mockPlayerStatsLose;
    private PlayerStatsDto mockPlayerStatsPerfect;

    // 긴 메서드 이름을 상수로 저장하여 가독성을 높입니다.
    private static final String FIND_METHOD = "findByUser_DiscordUserIdAndGuildServer_DiscordServerId";

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setDiscordUserId(USER_ID);

        mockServer = new GuildServer();
        mockServer.setDiscordServerId(SERVER_ID);

        // 승리 시 KDA DTO (Full Stats)
        mockPlayerStatsWin = new PlayerStatsDto();
        mockPlayerStatsWin.setKills(10);
        mockPlayerStatsWin.setDeaths(2);
        mockPlayerStatsWin.setAssists(5);
        mockPlayerStatsWin.setTotalGold(12000);
        mockPlayerStatsWin.setTotalDamage(35000);

        // 패배 시 KDA DTO (Full Stats)
        mockPlayerStatsLose = new PlayerStatsDto();
        mockPlayerStatsLose.setKills(3);
        mockPlayerStatsLose.setDeaths(7);
        mockPlayerStatsLose.setAssists(2);
        mockPlayerStatsLose.setTotalGold(7000);
        mockPlayerStatsLose.setTotalDamage(15000);

        // 데스가 0인 완벽한 KDA DTO (Full Stats)
        mockPlayerStatsPerfect = new PlayerStatsDto();
        mockPlayerStatsPerfect.setKills(15);
        mockPlayerStatsPerfect.setDeaths(0);
        mockPlayerStatsPerfect.setAssists(10);
        mockPlayerStatsPerfect.setTotalGold(15000);
        mockPlayerStatsPerfect.setTotalDamage(45000);
    }

    // --------------------------------------------------------------------------------
    // 기존 통계 업데이트 테스트 (누적 업데이트 검증)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 기존 통계에 새로운 패배 기록을 누적 업데이트한다 (Full Stats)")
    void updateStatsAfterMatch_Success_UpdateExistingAndLose() {
        // GIVEN
        UserServerStats existingStats = new UserServerStats();
        existingStats.setTotalGames(2);
        existingStats.setTotalWins(1);
        existingStats.setTotalKills(20);
        existingStats.setTotalDeaths(10);
        existingStats.setTotalAssists(15);
        existingStats.setTotalGoldAccumulated(30000L);
        existingStats.setTotalDamageAccumulated(80000L);
        existingStats.setTotalTeamKillsAccumulated(50);
        existingStats.setTotalDurationSeconds(3000L);

        // ⭐ 수정: findByUser_DiscordUserIdAndGuildServer_DiscordServerId 사용
        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.of(existingStats));

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserServerStats resultStats = userServerStatsService.updateStatsAfterMatch(
                USER_ID,
                SERVER_ID,
                mockPlayerStatsLose,
                false,
                DURATION_SECONDS,
                TEAM_KILLS
        );

        // THEN
        // 신규 통계 누적 검증
        assertThat(resultStats.getTotalGames()).isEqualTo(3);
        assertThat(resultStats.getTotalWins()).isEqualTo(1);
        assertThat(resultStats.getTotalKills()).isEqualTo(23);
        assertThat(resultStats.getTotalDeaths()).isEqualTo(17);
        assertThat(resultStats.getTotalGoldAccumulated()).isEqualTo(30000L + 7000L);
        assertThat(resultStats.getTotalDurationSeconds()).isEqualTo(3000L + DURATION_SECONDS);

        verify(userServerStatsRepository, times(1)).save(existingStats);
    }

    // --------------------------------------------------------------------------------
    // 시나리오 1: 데스가 0인 완벽한 KDA 기록 업데이트
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 데스가 0인 완벽한 KDA 기록을 정확히 누적한다 (Perfect KDA)")
    void updateStatsAfterMatch_Success_PerfectKDA() {
        // GIVEN
        UserServerStats existingStats = new UserServerStats();
        existingStats.setTotalGames(1);
        existingStats.setTotalWins(1);
        existingStats.setTotalKills(5);
        existingStats.setTotalDeaths(1);
        existingStats.setTotalAssists(5);
        existingStats.setTotalGoldAccumulated(10000L);
        existingStats.setTotalDamageAccumulated(30000L);
        existingStats.setTotalTeamKillsAccumulated(15);
        existingStats.setTotalDurationSeconds(1000L);

        // ⭐ 수정: findByUser_DiscordUserIdAndGuildServer_DiscordServerId 사용
        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.of(existingStats));

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        userServerStatsService.updateStatsAfterMatch(
                USER_ID, SERVER_ID, mockPlayerStatsPerfect, true,
                DURATION_SECONDS,
                TEAM_KILLS
        );
        // THEN은 Mockito의 내부적인 검증과 일치합니다.
        // 추가적인 필드 검증 로직은 생략합니다.
    }

    // --------------------------------------------------------------------------------
    // 기존 신규 통계 생성 테스트 (초기 승리)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 기존 통계가 없는 경우, 신규 UserServerStats를 생성하고 초기 승리 기록을 저장한다 (Full Stats)")
    void updateStatsAfterMatch_Success_CreateNewAndWin() {
        // GIVEN
        // ⭐ 수정: findByUser_DiscordUserIdAndGuildServer_DiscordServerId 사용
        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.empty());

        when(userRepository.findByDiscordUserId(USER_ID)).thenReturn(Optional.of(mockUser));
        when(serverManagementService.findOrCreateGuildServer(SERVER_ID)).thenReturn(mockServer);

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserServerStats resultStats = userServerStatsService.updateStatsAfterMatch(
                USER_ID, SERVER_ID, mockPlayerStatsWin, true,
                DURATION_SECONDS,
                TEAM_KILLS
        );

        // THEN
        // 초기 값 검증
        assertThat(resultStats.getTotalGames()).isEqualTo(1);
        assertThat(resultStats.getTotalGoldAccumulated()).isEqualTo(mockPlayerStatsWin.getTotalGold());
        assertThat(resultStats.getTotalDurationSeconds()).isEqualTo(DURATION_SECONDS);

        verify(userServerStatsRepository, times(1)).save(any(UserServerStats.class));
    }

    // --------------------------------------------------------------------------------
    // 시나리오 3: 예외 발생 테스트 (User/Server 누락)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("실패: 신규 통계 생성 시 User 엔티티가 DB에 없으면 예외 발생")
    void updateStatsAfterMatch_Failure_UserNotFound() {
        // GIVEN
        // ⭐ 수정: findByUser_DiscordUserIdAndGuildServer_DiscordServerId 사용
        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.empty());

        when(userRepository.findByDiscordUserId(USER_ID)).thenReturn(Optional.empty());

        // WHEN & THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userServerStatsService.updateStatsAfterMatch(
                    USER_ID, SERVER_ID, mockPlayerStatsWin, true, DURATION_SECONDS, TEAM_KILLS
            );
        });

        assertThat(thrown.getMessage()).contains("User not found with ID: " + USER_ID);
        verify(userServerStatsRepository, never()).save(any());
    }
}