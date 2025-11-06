package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.ChampionStats;
import com.discordBot.demo.domain.entity.ChampionStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChampionStatsRepository extends JpaRepository<ChampionStats, ChampionStatsId> {

    /**
     * 특정 유저의 특정 서버 내 모든 챔피언 통계를 조회합니다. (UserSearchService에서 사용)
     */
    List<ChampionStats> findAllByUser_DiscordUserIdAndGuildServer_DiscordServerId(Long userId, Long serverId);


    /**
     * 기존 findUserAllChampionStats 쿼리 유지
     */
    @Query("SELECT c FROM ChampionStats c WHERE c.user.discordUserId = :userId AND c.guildServer.discordServerId = :serverId")
    List<ChampionStats> findUserAllChampionStats(@Param("userId") Long userId, @Param("serverId") Long serverId);


    /**
     * ChampionStatsServiceImpl의 update 로직에서 사용됩니다.
     */
    @Query("SELECT c FROM ChampionStats c WHERE c.user.discordUserId = :userId " +
            "AND c.guildServer.discordServerId = :serverId " +
            "AND c.champion.championId = :championId")
    Optional<ChampionStats> findChampionStatsByCompositeKeys(
            @Param("userId") Long userId,
            @Param("serverId") Long serverId,
            @Param("championId") Long championId
    );
}