package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.ChampionStats;
import com.discordBot.demo.domain.entity.ChampionStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChampionStatsRepository extends JpaRepository<ChampionStats, ChampionStatsId> {

    /**
     * 특정 유저의 특정 서버 내 모든 챔피언 통계를 조회합니다.
     * User Lookup 기능에서 챔피언별 통계를 출력할 때 사용됩니다.
     * @param userId Discord User ID
     * @param serverId Discord Server ID
     * @return 해당 유저의 모든 ChampionStats 리스트
     */
    @Query("SELECT c FROM ChampionStats c WHERE c.user.discordUserId = :userId AND c.guildServer.discordServerId = :serverId")
    List<ChampionStats> findUserAllChampionStats(@Param("userId") Long userId, @Param("serverId") Long serverId);
}