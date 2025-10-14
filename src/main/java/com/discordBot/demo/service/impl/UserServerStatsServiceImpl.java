package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.entity.UserServerStats;
import com.discordBot.demo.domain.entity.UserServerStatsId;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
import com.discordBot.demo.service.ServerManagementService;
import com.discordBot.demo.service.UserServerStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServerStatsServiceImpl implements UserServerStatsService {

    private final UserServerStatsRepository userServerStatsRepository;
    private final UserRepository userRepository;
    private final ServerManagementService serverManagementService;

    @Override
    public UserServerStats updateStatsAfterMatch(
            Long userId,
            Long serverId,
            PlayerStatsDto playerStatsDto,
            boolean isWin) {

        // 1. 기존 통계 데이터 조회
        // User ID와 Server ID를 복합키로 사용하여 기존 통계 기록을 찾습니다.
        UserServerStats stats = userServerStatsRepository
                .findByUser_DiscordUserIdAndGuildServer_DiscordServerId(userId, serverId)
                // 통계 기록이 없으면 새로 생성합니다.
                .orElseGet(() -> createNewUserServerStats(userId, serverId));

        // 2. 통계 업데이트 (K/D/A, 게임 수, 승리 수 증가)
        stats.addStats(
                playerStatsDto.getKills(),
                playerStatsDto.getDeaths(),
                playerStatsDto.getAssists(),
                isWin
        );

        // 3. 변경 사항 저장
        // @Transactional 어노테이션으로 인해 트랜잭션 종료 시 자동으로 저장될 수 있으나,
        // 명시적으로 save를 호출하여 즉시 저장하거나 새로운 엔티티를 관리 상태로 만듭니다.
        return userServerStatsRepository.save(stats);
    }

    /**
     * UserServerStats 레코드가 없는 경우 새로 생성하고 연관관계를 설정합니다.
     */
    private UserServerStats createNewUserServerStats(Long userId, Long serverId) {

        // 1. 연관 엔티티 조회
        // User 엔티티는 반드시 존재해야 합니다.
        User user = userRepository.findByDiscordUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // GuildServer 엔티티 조회 (ServerManagementService 재활용)
        GuildServer guildServer = serverManagementService.findOrCreateGuildServer(serverId);

        // 2. 복합 키 생성 및 UserServerStats 초기화
        UserServerStatsId id = new UserServerStatsId();
        id.setUser(userId);
        id.setGuildServer(serverId);

        UserServerStats newStats = new UserServerStats();
        newStats.setId(id);

        // 3. 연관관계 설정
        newStats.setUser(user);
        newStats.setGuildServer(guildServer);

        return newStats;
    }
}