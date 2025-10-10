package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LolAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LolAccountRepository extends JpaRepository<LolAccount, Long> {

    // 1. 서버별 롤 계정 등록 확인 및 조회
    // (서버별 중복 확인에 사용)
    Optional<LolAccount> findByGameNameAndTagLineAndGuildServer_DiscordServerId(
            String gameName,
            String tagLine,
            Long discordServerId
    );

    /**
     * OCR 힌트 제공을 위해 해당 서버에 등록된 모든 롤 계정을 조회합니다.
     */
    // 2. OCR 힌트 제공을 위한 서버별 전체 조회
    List<LolAccount> findAllByGuildServer_DiscordServerId(Long serverId);
}
