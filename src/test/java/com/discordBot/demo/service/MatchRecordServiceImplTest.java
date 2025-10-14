package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.MatchRecord;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.MatchRecordRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchRecordServiceImplTest {

    @InjectMocks
    private MatchRecordServiceImpl matchRecordService;

    @Mock private MatchRecordRepository matchRecordRepository;
    @Mock private ServerManagementService serverManagementService;
    @Mock private LolAccountRepository lolAccountRepository;
    @Mock private EntityManager em;

    private static final Long DISCORD_SERVER_ID = 500L;
    private static final String WINNER_TEAM = "BLUE";
    private GuildServer mockGuildServer;

    @BeforeEach
    void setUp() {
        mockGuildServer = new GuildServer();
        mockGuildServer.setDiscordServerId(DISCORD_SERVER_ID);
        mockGuildServer.setServerName("TestServer");

        when(serverManagementService.findOrCreateGuildServer(DISCORD_SERVER_ID))
                .thenReturn(mockGuildServer);
    }

    // --------------------------------------------------------------------------------
    // 성공 케이스: 모든 계정이 등록되어 있는 경우
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 모든 계정이 등록된 경우 경기 기록이 저장되어야 한다")
    void registerMatch_Success_AllAccountsRegistered() {
        // GIVEN
        LolAccount registeredLolAccount1 = createMockLolAccount("Faker", "KR1", 1001L);
        LolAccount registeredLolAccount2 = createMockLolAccount("Gumayusi", "KR1", 1002L);

        List<PlayerStatsDto> playerStatsList = List.of(
                createPlayerStatsDto("Faker", "KR1", "BLUE", 10, 2, 5),
                createPlayerStatsDto("Gumayusi", "KR1", "RED", 3, 7, 2)
        );

        MatchRegistrationDto matchDto = new MatchRegistrationDto();
        matchDto.setServerId(DISCORD_SERVER_ID);
        matchDto.setWinnerTeam(WINNER_TEAM);
        matchDto.setPlayerStatsList(playerStatsList);

        // 2. Repository Mocking: 모든 계정이 Optional.of(LolAccount)를 반환해야 함
        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq("Faker"), eq("KR1"), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.of(registeredLolAccount1));

        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq("Gumayusi"), eq("KR1"), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.of(registeredLolAccount2));

        // 3. EntityManager Mocking: getReference는 프록시 객체를 반환해야 함
        when(em.getReference(eq(GuildServer.class), eq(DISCORD_SERVER_ID)))
                .thenReturn(mockGuildServer);

        // 4. MatchRecord 저장 Mocking (저장 호출을 확인하기 위함)
        when(matchRecordRepository.save(any(MatchRecord.class)))
                .thenAnswer(invocation -> {
                    // 저장 시 PlayerStats가 MatchRecord에 올바르게 추가되었는지 확인 (4. 검증 완료 후)
                    MatchRecord saved = invocation.getArgument(0);
                    assertThat(saved.getPlayerStats()).hasSize(2);
                    return saved;
                });

        // WHEN
        MatchRecord savedRecord = matchRecordService.registerMatch(matchDto);

        // THEN
        // 1. MatchRecord가 성공적으로 저장되었는지 확인
        assertThat(savedRecord).isNotNull();
        assertThat(savedRecord.getWinnerTeam()).isEqualTo(WINNER_TEAM);
        // ⭐ getPlayerStats() 대신 올바른 메서드명(getPlayerStatsList()) 사용
        assertThat(savedRecord.getPlayerStats()).hasSize(2);

        // 2. 핵심 메서드 호출 확인
        verify(matchRecordRepository, times(1)).save(any(MatchRecord.class));
        verify(serverManagementService, times(1)).findOrCreateGuildServer(DISCORD_SERVER_ID);

        // ⭐ 수정: 순회 두 번(검증 + 저장)이므로 2명 * 2회 = 4회 호출이 예상됨
        verify(lolAccountRepository, times(4)).findByGameNameAndTagLineAndGuildServer_DiscordServerId(anyString(), anyString(), eq(DISCORD_SERVER_ID));
    }

    // --------------------------------------------------------------------------------
    // 실패 케이스: 미등록 계정이 포함된 경우
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("실패: 미등록 계정이 포함된 경우 IllegalArgumentException이 발생해야 한다")
    void registerMatch_Failure_UnregisteredAccount() {
        // GIVEN
        LolAccount registeredLolAccount = createMockLolAccount("Faker", "KR1", 1001L);

        List<PlayerStatsDto> playerStatsList = List.of(
                createPlayerStatsDto("Faker", "KR1", "BLUE", 10, 2, 5),
                createPlayerStatsDto("Teemo", "NA1", "RED", 3, 7, 2)
        );

        MatchRegistrationDto matchDto = new MatchRegistrationDto();
        matchDto.setServerId(DISCORD_SERVER_ID);
        matchDto.setWinnerTeam(WINNER_TEAM);
        matchDto.setPlayerStatsList(playerStatsList);

        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq("Faker"), eq("KR1"), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.of(registeredLolAccount));

        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq("Teemo"), eq("NA1"), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.empty()); // Teemo는 미등록

        // WHEN & THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            matchRecordService.registerMatch(matchDto);
        });

        // ⭐ 수정: Assertions.contain()을 두 번 사용하여 Markdown 오류를 피하고 핵심 메시지 검증
        assertThat(thrown.getMessage())
                .contains("❌ 오류: 이 서버에 등록되지 않은 롤 계정이 포함되어 있습니다. 먼저 `/register` 명령어로 해당 계정을 등록해 주세요.")
                .contains("**[미등록 계정 목록]**\nTeemo#NA1");

        // DB 저장 호출이 없어야 함
        verify(matchRecordRepository, never()).save(any());

        // 검증 단계에서 호출 횟수 확인 (Faker 1회, Teemo 1회 = 총 2회)
        verify(lolAccountRepository, times(2)).findByGameNameAndTagLineAndGuildServer_DiscordServerId(anyString(), anyString(), eq(DISCORD_SERVER_ID));
    }

    // --------------------------------------------------------------------------------
    // 헬퍼 메서드: Mock 객체 생성을 위한 유틸리티
    // --------------------------------------------------------------------------------

    private LolAccount createMockLolAccount(String gameName, String tagLine, Long discordUserId) {
        User mockUser = new User();
        mockUser.setDiscordUserId(discordUserId);

        LolAccount account = new LolAccount();
        account.setGameName(gameName);
        account.setTagLine(tagLine);
        account.setUser(mockUser);

        return account;
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
}