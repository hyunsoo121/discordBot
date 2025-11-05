package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LolAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LolAccountRepository extends JpaRepository<LolAccount, Long> {

    // 1. 서버별 롤 계정 등록 확인 및 조회 (기존 로직 유지)
    Optional<LolAccount> findByGameNameAndTagLineAndGuildServer_DiscordServerId(
            String gameName,
            String tagLine,
            Long discordServerId
    );

    // 2. OCR 힌트 제공을 위한 서버별 전체 조회 (기존 로직 유지)
    List<LolAccount> findAllByGuildServer_DiscordServerId(Long serverId);

    // ⭐ 3. Riot ID (GameName+TagLine)만으로 LolAccount를 조회 (UserSearchService에서 사용)
    Optional<LolAccount> findByGameNameAndTagLine(String gameName, String tagLine);
}