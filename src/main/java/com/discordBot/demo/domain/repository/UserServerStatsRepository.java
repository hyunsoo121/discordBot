package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.UserServerStats;
import com.discordBot.demo.domain.entity.UserServerStatsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserServerStatsRepository extends JpaRepository<UserServerStats, UserServerStatsId> {

    /**
     * 특정 유저의 특정 서버 통계 데이터를 조회합니다.
     * 복합키 대신 엔티티 필드명을 사용하여 가독성을 높였습니다.
     * @param userId User 엔티티의 ID (Discord User ID)
     * @param guildServerId GuildServer 엔티티의 ID (Discord Server ID)
     * @return UserServerStats Optional 객체
     */
    Optional<UserServerStats> findByUser_DiscordUserIdAndGuildServer_DiscordServerId(
            Long userId,
            Long guildServerId
    );

    /**
     * 특정 서버(길드)에 속한 모든 유저의 통계 데이터를 조회합니다.
     * 랭킹 목록을 구성할 때 사용됩니다.
     * @param guildServerId GuildServer 엔티티의 ID (Discord Server ID)
     * @return 해당 서버의 모든 UserServerStats 리스트
     */
    List<UserServerStats> findAllByGuildServer_DiscordServerId(Long guildServerId);
}