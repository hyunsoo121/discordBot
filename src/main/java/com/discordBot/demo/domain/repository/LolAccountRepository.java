package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LolAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LolAccountRepository extends JpaRepository<LolAccount, Long> {

    // 1. 서버별 롤 계정 등록 확인 및 조회 (기존 로직 유지)
    Optional<LolAccount> findByGameNameAndTagLineAndGuildServer_DiscordServerId(
            String gameName,
            String tagLine,
            Long discordServerId
    );

    @Query("SELECT DISTINCT la FROM LolAccount la " +
            "LEFT JOIN FETCH la.preferredLines " + // ⭐ EAGER 로드 추가
            "WHERE la.guildServer.discordServerId = :serverId")
    List<LolAccount> findAllByGuildServer_DiscordServerId(@Param("serverId") Long serverId);

    // ⭐ 3. Riot ID (GameName+TagLine)만으로 LolAccount를 조회 (UserSearchService에서 사용)
    Optional<LolAccount> findByGameNameAndTagLine(String gameName, String tagLine);
}