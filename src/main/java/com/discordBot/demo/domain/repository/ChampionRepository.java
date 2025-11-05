package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.Champion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChampionRepository extends JpaRepository<Champion, Long> {

    // Riot API에서 사용하는 영문 챔피언 키(e.g., Aatrox)로 챔피언 정보를 조회.
    Optional<Champion> findByChampionKey(String championKey);

    // 챔피언의 현재 이름(한글 이름)으로 챔피언 정보를 조회.
    Optional<Champion> findByName(String name);
}