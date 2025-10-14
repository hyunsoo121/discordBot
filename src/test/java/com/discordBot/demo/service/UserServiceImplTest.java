package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.service.impl.UserServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService; // 테스트 대상

    @Mock private UserRepository userRepository;
    @Mock private LolAccountRepository lolAccountRepository;
    @Mock private RiotApiService riotApiService;
    @Mock private ServerManagementService serverManagementService;

    // 테스트용 상수 데이터
    private static final Long TARGET_DISCORD_ID = 123456789L;
    private static final Long OTHER_DISCORD_ID = 987654321L;
    private static final Long DISCORD_SERVER_ID = 400L;
    private static final String GAME_NAME = "Faker";
    private static final String TAG_LINE = "KR1";
    private static final String PUUID = "test-puuid-faker";

    private RiotAccountDto riotAccountDto;
    private User targetUser;
    private User otherUser;
    private GuildServer guildServer;

    @BeforeEach
    void setUp() {
        // RiotAccountDto (빌더 사용은 외부 DTO이므로 유지한다고 가정)
        // 만약 이 DTO도 빌더가 없다면, DTO의 생성자/Setter를 사용해야 합니다.
        riotAccountDto = new RiotAccountDto();
        riotAccountDto.setGameName(GAME_NAME);
        riotAccountDto.setTagLine(TAG_LINE);
        riotAccountDto.setPuuid(PUUID);

        // User 엔티티 (NoArgsConstructor 및 Setter 사용)
        targetUser = new User();
        targetUser.setDiscordUserId(TARGET_DISCORD_ID);

        otherUser = new User();
        otherUser.setDiscordUserId(OTHER_DISCORD_ID);

        // GuildServer 엔티티 (NoArgsConstructor 및 Setter 사용)
        guildServer = new GuildServer();
        guildServer.setDiscordServerId(DISCORD_SERVER_ID);
        guildServer.setServerName("TestServer");
    }

    // --------------------------------------------------------------------------------
    // registerLolNickname 테스트 (관리자 대리 등록, 서버별)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: Riot 검증 후 서버에 신규 계정 등록")
    void registerLolNickname_Success_NewAccount() {
        // GIVEN
        when(riotApiService.verifyNickname(eq(GAME_NAME), eq(TAG_LINE))).thenReturn(Optional.of(riotAccountDto));

        when(userRepository.findByDiscordUserId(TARGET_DISCORD_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(targetUser);

        when(serverManagementService.findOrCreateGuildServer(DISCORD_SERVER_ID)).thenReturn(guildServer);

        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq(GAME_NAME), eq(TAG_LINE), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.empty());

        when(lolAccountRepository.save(any(LolAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));


        // WHEN
        String result = userService.registerLolNickname(TARGET_DISCORD_ID, GAME_NAME, TAG_LINE, DISCORD_SERVER_ID);

        // THEN
        assertThat(result).contains("성공적으로 등록되었습니다!");
        verify(userRepository, times(1)).save(any(User.class));
        verify(lolAccountRepository, times(1)).save(any(LolAccount.class));
    }

    @Test
    @DisplayName("성공: 서버 내 유저가 없는 기존 계정(임시 계정)에 사용자 연결")
    void registerLolNickname_Success_LinkExistingAccountWithoutUser() {
        // GIVEN
        // LolAccount 엔티티 생성 (NoArgsConstructor 및 Setter 사용)
        LolAccount existingAccount = new LolAccount();
        existingAccount.setGameName(GAME_NAME);
        existingAccount.setTagLine(TAG_LINE);
        existingAccount.setUser(null);
        existingAccount.setGuildServer(guildServer);

        when(riotApiService.verifyNickname(eq(GAME_NAME), eq(TAG_LINE))).thenReturn(Optional.of(riotAccountDto));

        when(userRepository.findByDiscordUserId(TARGET_DISCORD_ID)).thenReturn(Optional.of(targetUser));

        when(serverManagementService.findOrCreateGuildServer(DISCORD_SERVER_ID)).thenReturn(guildServer);

        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq(GAME_NAME), eq(TAG_LINE), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.of(existingAccount));

        when(lolAccountRepository.save(any(LolAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        String result = userService.registerLolNickname(TARGET_DISCORD_ID, GAME_NAME, TAG_LINE, DISCORD_SERVER_ID);

        // THEN
        assertThat(result).contains("대상 유저에게 연결되었습니다!");
        verify(lolAccountRepository, times(1)).save(any(LolAccount.class));
        verify(userRepository, never()).save(any(User.class));
        assertThat(existingAccount.getUser()).isEqualTo(targetUser);
    }

    @Test
    @DisplayName("실패: Riot API에 닉네임이 존재하지 않음")
    void registerLolNickname_Failure_NicknameNotFound() {
        // GIVEN
        when(riotApiService.verifyNickname(eq(GAME_NAME), eq(TAG_LINE))).thenReturn(Optional.empty());

        // WHEN & THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerLolNickname(TARGET_DISCORD_ID, GAME_NAME, TAG_LINE, DISCORD_SERVER_ID);
        });

        assertThat(thrown.getMessage()).contains("Riot Games에 **Faker#KR1**에 해당하는 계정이 존재하지 않습니다.");
        verify(lolAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("실패: 동일 서버 내 이미 다른 유저가 소유한 계정 등록 시도 (소유권 충돌)")
    void registerLolNickname_Failure_OwnerConflict_SameServer() {
        // GIVEN
        // LolAccount 엔티티 생성 (NoArgsConstructor 및 Setter 사용)
        LolAccount existingAccount = new LolAccount();
        existingAccount.setGameName(GAME_NAME);
        existingAccount.setTagLine(TAG_LINE);
        existingAccount.setUser(otherUser);
        existingAccount.setGuildServer(guildServer);

        when(riotApiService.verifyNickname(eq(GAME_NAME), eq(TAG_LINE))).thenReturn(Optional.of(riotAccountDto));

        when(userRepository.findByDiscordUserId(TARGET_DISCORD_ID)).thenReturn(Optional.of(targetUser));

        when(serverManagementService.findOrCreateGuildServer(DISCORD_SERVER_ID)).thenReturn(guildServer);

        when(lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                eq(GAME_NAME), eq(TAG_LINE), eq(DISCORD_SERVER_ID)))
                .thenReturn(Optional.of(existingAccount));

        // WHEN & THEN
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerLolNickname(TARGET_DISCORD_ID, GAME_NAME, TAG_LINE, DISCORD_SERVER_ID);
        });

        assertThat(thrown.getMessage()).contains("이 서버 내 롤 계정 **Faker#KR1**은 이미 다른 사용자에게 등록되어 있어 소유권을 변경할 수 없습니다.");
        verify(lolAccountRepository, never()).save(any());
    }

    // --------------------------------------------------------------------------------
    // linkExistingAccount 테스트 (유틸리티 메서드 - 변경 사항 없으므로 빌더 제거만 반영)
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("성공: 기존 LolAccount에 Discord User 연결 (linkExistingAccount)")
    void linkExistingAccount_Success() {
        // GIVEN
        Long lolAccountId = 1L;
        // LolAccount 엔티티 생성 (NoArgsConstructor 및 Setter 사용)
        LolAccount lolAccount = new LolAccount();
        lolAccount.setLolId(lolAccountId);
        lolAccount.setGameName(GAME_NAME);
        lolAccount.setTagLine(TAG_LINE);
        lolAccount.setUser(null);

        when(lolAccountRepository.findById(lolAccountId)).thenReturn(Optional.of(lolAccount));
        when(userRepository.findByDiscordUserId(TARGET_DISCORD_ID)).thenReturn(Optional.of(targetUser));

        when(lolAccountRepository.save(any(LolAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        String result = userService.linkExistingAccount(TARGET_DISCORD_ID, lolAccountId);

        // THEN
        assertThat(result).contains("성공적으로 연결되었습니다!");
        assertThat(lolAccount.getUser()).isEqualTo(targetUser);
        verify(lolAccountRepository, times(1)).save(lolAccount);
    }
}