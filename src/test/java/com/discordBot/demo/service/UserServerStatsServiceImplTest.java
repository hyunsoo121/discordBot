package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.entity.UserServerStats;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private User mockUser;
    private GuildServer mockServer;
    private PlayerStatsDto mockPlayerStatsWin;
    private PlayerStatsDto mockPlayerStatsLose;
    private PlayerStatsDto mockPlayerStatsPerfect; // ⭐ 데스가 0인 경우 추가

    @BeforeEach
    void setUp() {
        // Mock 엔티티 설정
        mockUser = new User();
        mockUser.setDiscordUserId(USER_ID);

        mockServer = new GuildServer();
        mockServer.setDiscordServerId(SERVER_ID);

        // 승리 시 KDA DTO
        mockPlayerStatsWin = new PlayerStatsDto();
        mockPlayerStatsWin.setKills(10);
        mockPlayerStatsWin.setDeaths(2);
        mockPlayerStatsWin.setAssists(5);

        // 패배 시 KDA DTO
        mockPlayerStatsLose = new PlayerStatsDto();
        mockPlayerStatsLose.setKills(3);
        mockPlayerStatsLose.setDeaths(7);
        mockPlayerStatsLose.setAssists(2);

        // ⭐ 데스가 0인 완벽한 KDA DTO
        mockPlayerStatsPerfect = new PlayerStatsDto();
        mockPlayerStatsPerfect.setKills(15);
        mockPlayerStatsPerfect.setDeaths(0);
        mockPlayerStatsPerfect.setAssists(10);
    }

    // --------------------------------------------------------------------------------
    // 기존 통계 업데이트 테스트 (누적 업데이트 검증)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 기존 통계에 새로운 패배 기록을 누적 업데이트한다")
    void updateStatsAfterMatch_Success_UpdateExistingAndLose() {
        // GIVEN
        UserServerStats existingStats = new UserServerStats();
        existingStats.setTotalGames(2);
        existingStats.setTotalWins(1);
        existingStats.setTotalKills(20);
        existingStats.setTotalDeaths(10);
        existingStats.setTotalAssists(15);

        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.of(existingStats));

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserServerStats resultStats = userServerStatsService.updateStatsAfterMatch(
                USER_ID, SERVER_ID, mockPlayerStatsLose, false // 패배 기록
        );

        // THEN
        // 최종 누적 통계 값 검증 (기존 값 + 새로운 값)
        assertThat(resultStats.getTotalGames()).isEqualTo(3); // 2 + 1
        assertThat(resultStats.getTotalWins()).isEqualTo(1);  // 1 + 0 (패배)
        assertThat(resultStats.getTotalKills()).isEqualTo(23); // 20 + 3
        assertThat(resultStats.getTotalDeaths()).isEqualTo(17); // 10 + 7
        assertThat(resultStats.getTotalAssists()).isEqualTo(17); // 15 + 2

        verify(userServerStatsRepository, times(1)).save(existingStats);
    }

    // --------------------------------------------------------------------------------
    // ⭐ 시나리오 1: 데스가 0인 완벽한 KDA 기록 업데이트
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 데스가 0인 완벽한 KDA 기록을 정확히 누적한다")
    void updateStatsAfterMatch_Success_PerfectKDA() {
        // GIVEN
        UserServerStats existingStats = new UserServerStats();
        existingStats.setTotalGames(1);
        existingStats.setTotalWins(1);
        existingStats.setTotalKills(5);
        existingStats.setTotalDeaths(1);
        existingStats.setTotalAssists(5);

        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.of(existingStats));

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserServerStats resultStats = userServerStatsService.updateStatsAfterMatch(
                USER_ID, SERVER_ID, mockPlayerStatsPerfect, true // 승리 기록 (데스 0)
        );

        // THEN
        // 최종 누적 통계 값 검증
        assertThat(resultStats.getTotalGames()).isEqualTo(2); // 1 + 1
        assertThat(resultStats.getTotalWins()).isEqualTo(2);  // 1 + 1
        assertThat(resultStats.getTotalKills()).isEqualTo(20); // 5 + 15
        assertThat(resultStats.getTotalDeaths()).isEqualTo(1); // 1 + 0
        assertThat(resultStats.getTotalAssists()).isEqualTo(15); // 5 + 10

        verify(userServerStatsRepository, times(1)).save(existingStats);
    }

    // --------------------------------------------------------------------------------
    // ⭐ 시나리오 2: 경계 값 테스트 (큰 값 누적)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 큰 누적 통계 값에 정상적으로 새로운 통계를 더한다")
    void updateStatsAfterMatch_Success_BoundaryValues() {
        // GIVEN
        UserServerStats existingStats = new UserServerStats();
        existingStats.setTotalGames(999);
        existingStats.setTotalWins(500);
        existingStats.setTotalKills(10000);
        existingStats.setTotalDeaths(5000);
        existingStats.setTotalAssists(12000);

        PlayerStatsDto bigKdaDto = new PlayerStatsDto();
        bigKdaDto.setKills(50);
        bigKdaDto.setDeaths(1);
        bigKdaDto.setAssists(70);

        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.of(existingStats));

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserServerStats resultStats = userServerStatsService.updateStatsAfterMatch(
                USER_ID, SERVER_ID, bigKdaDto, true
        );

        // THEN
        // 최종 누적 통계 값 검증
        assertThat(resultStats.getTotalGames()).isEqualTo(1000);
        assertThat(resultStats.getTotalWins()).isEqualTo(501);
        assertThat(resultStats.getTotalKills()).isEqualTo(10050);
        assertThat(resultStats.getTotalDeaths()).isEqualTo(5001);
        assertThat(resultStats.getTotalAssists()).isEqualTo(12070);

        verify(userServerStatsRepository, times(1)).save(existingStats);
    }

    // --------------------------------------------------------------------------------
    // ⭐ 시나리오 3: 예외 발생 테스트 (User/Server 누락)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("실패: 신규 통계 생성 시 User 엔티티가 DB에 없으면 예외 발생")
    void updateStatsAfterMatch_Failure_UserNotFound() {
        // GIVEN
        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.empty());

        // UserRepository가 유저를 찾지 못함
        when(userRepository.findByDiscordUserId(USER_ID)).thenReturn(Optional.empty());

        // WHEN & THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userServerStatsService.updateStatsAfterMatch(USER_ID, SERVER_ID, mockPlayerStatsWin, true);
        });

        assertThat(thrown.getMessage()).contains("User not found with ID: " + USER_ID);
        verify(userServerStatsRepository, never()).save(any());
        verify(serverManagementService, never()).findOrCreateGuildServer(anyLong()); // 서버 생성 로직도 호출되면 안 됨
    }

    // --------------------------------------------------------------------------------
    // 기존 신규 통계 생성 테스트 (초기 승리)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 기존 통계가 없는 경우, 신규 UserServerStats를 생성하고 초기 승리 기록을 저장한다")
    void updateStatsAfterMatch_Success_CreateNewAndWin() {
        // GIVEN
        when(userServerStatsRepository.findByUser_DiscordUserIdAndGuildServer_DiscordServerId(USER_ID, SERVER_ID))
                .thenReturn(Optional.empty());

        when(userRepository.findByDiscordUserId(USER_ID)).thenReturn(Optional.of(mockUser));
        when(serverManagementService.findOrCreateGuildServer(SERVER_ID)).thenReturn(mockServer);

        when(userServerStatsRepository.save(any(UserServerStats.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        UserServerStats resultStats = userServerStatsService.updateStatsAfterMatch(
                USER_ID, SERVER_ID, mockPlayerStatsWin, true // 승리 기록
        );

        // THEN
        assertThat(resultStats.getTotalGames()).isEqualTo(1);
        assertThat(resultStats.getTotalWins()).isEqualTo(1);
        assertThat(resultStats.getTotalKills()).isEqualTo(10);
        assertThat(resultStats.getTotalDeaths()).isEqualTo(2);

        verify(userServerStatsRepository, times(1)).save(any(UserServerStats.class));
    }
}