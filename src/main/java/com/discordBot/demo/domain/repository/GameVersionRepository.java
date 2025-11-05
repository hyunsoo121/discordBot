package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.GameVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GameVersionRepository extends JpaRepository<GameVersion, Long> {

    /**
     * 가장 최근에 업데이트된 GameVersion 레코드를 조회합니다.
     */
    Optional<GameVersion> findTopByOrderByUpdateDateDesc();
}
