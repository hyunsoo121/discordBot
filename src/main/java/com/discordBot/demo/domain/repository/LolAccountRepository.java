package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LolAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LolAccountRepository extends JpaRepository<LolAccount, Long> {

    Optional<LolAccount> findByGameNameAndTagLine(String gameName, String tagLine);

    /**
     * 특정 Discord 서버에 등록된 사용자에게 연결된 모든 LolAccount를 조회합니다.
     * * LolAccount -> User -> GuildServer의 다대다 관계를 조인하여 필터링합니다.
     * * GuildServer 엔티티의 PK인 'discordServerId'를 기준으로 필터링합니다.
     */
    @Query("SELECT la FROM LolAccount la JOIN la.user u JOIN u.guildServers gs WHERE gs.discordServerId = :serverId")
    List<LolAccount> findAllByServerId(@Param("serverId") Long serverId);
}
