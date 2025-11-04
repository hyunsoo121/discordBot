package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.Champion;

import java.util.List;
import java.util.Optional;

public interface ChampionService {

    /**
     * Riot Data Dragon API를 통해 챔피언 데이터를 최신 버전으로 업데이트합니다.
     * DB에 데이터가 없거나 버전이 변경되었을 때 실행됩니다.
     */
    void updateChampionDataIfNecessary();

    /**
     * 챔피언 이름(한글) 또는 Key(영문)을 기반으로 Champion 엔티티를 조회합니다.
     * @param identifier 챔피언 이름 또는 Key
     * @return Champion 엔티티
     */
    Optional<Champion> findChampionByIdentifier(String identifier);
    List<String> getAllChampionNamesForHint();

}