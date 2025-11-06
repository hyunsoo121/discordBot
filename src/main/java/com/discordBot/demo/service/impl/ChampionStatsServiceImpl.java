package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.ChampionSearchDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.Champion;
import com.discordBot.demo.domain.entity.ChampionStats;
import com.discordBot.demo.domain.entity.ChampionStatsId;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.ChampionStatsRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.service.ChampionService;
import com.discordBot.demo.service.ChampionStatsService;
import com.discordBot.demo.service.ServerManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChampionStatsServiceImpl implements ChampionStatsService {

    private final ChampionStatsRepository championStatsRepository;
    private final ChampionService championService;
    private final UserRepository userRepository;
    private final ServerManagementService serverManagementService;

    @Override
    @Transactional
    public void updateChampionStats(
            String championName,
            Long userId,
            Long serverId,
            PlayerStatsDto playerStatsDto,
            boolean isWin,
            long gameDurationSeconds,
            int teamTotalKills
    ) {
        // 1. 챔피언 엔티티 조회 (FK 연결 필요)
        Champion champion = championService.findChampionByIdentifier(championName)
                .orElseThrow(() -> new IllegalArgumentException("❌ 챔피언 [" + championName + "] 정보를 찾을 수 없습니다. (ChampionService 오류)"));

        // 2. 대상 유저 및 서버 엔티티 조회
        User user = userRepository.findByDiscordUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 사용자 ID를 찾을 수 없습니다: " + userId));

        // GuildServer는 Proxy를 사용해도 되지만, 통일성을 위해 findOrCreate 사용
        GuildServer guildServer = serverManagementService.findOrCreateGuildServer(serverId);

        // 3. 기존 ChampionStats 레코드 조회
        // NOTE: findChampionStatsByCompositeKeys는 Repository에 JPQL로 정의되어 있어야 합니다.
        ChampionStats stats = championStatsRepository.findChampionStatsByCompositeKeys(
                userId,
                serverId,
                champion.getChampionId()
        ).orElseGet(() -> createNewChampionStats(user, guildServer, champion));

        // 4. 통계 누적
        stats.addStats(
                playerStatsDto.getKills(), playerStatsDto.getDeaths(), playerStatsDto.getAssists(),
                isWin,
                playerStatsDto.getTotalGold(), playerStatsDto.getTotalDamage(),
                teamTotalKills, gameDurationSeconds // DPM, GPM, KP 계산에 필요한 데이터 전달
        );

        championStatsRepository.save(stats);
    }

    /**
     * 새로운 ChampionStats 레코드를 생성하고 복합 키를 설정합니다.
     */
    private ChampionStats createNewChampionStats(User user, GuildServer guildServer, Champion champion) {
        ChampionStatsId id = new ChampionStatsId();
        id.setUser(user.getDiscordUserId());
        id.setGuildServer(guildServer.getDiscordServerId());
        id.setChampion(champion.getChampionId());

        ChampionStats newStats = new ChampionStats();
        newStats.setId(id);
        newStats.setUser(user);
        newStats.setGuildServer(guildServer);
        newStats.setChampion(champion);
        return newStats;
    }


    @Override
    @Transactional(readOnly = true)
    public List<ChampionSearchDto> getUserChampionStats(Long userId, Long serverId) {

        // ChampionStatsRepository에 정의된 findUserAllChampionStats 쿼리를 사용
        List<ChampionStats> allChampionStats = championStatsRepository.findUserAllChampionStats(userId, serverId);

        // ChampionSearchDto로 변환하여 반환 (계산된 KDA, GPM 등 포함)
        return allChampionStats.stream()
                .map(ChampionSearchDto::from)
                .collect(Collectors.toList());
    }
}