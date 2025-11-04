package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.entity.Champion;
import com.discordBot.demo.domain.entity.GameVersion;
import com.discordBot.demo.domain.repository.ChampionRepository;
import com.discordBot.demo.domain.repository.GameVersionRepository;
import com.discordBot.demo.service.ChampionService;
import com.discordBot.demo.service.RiotApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ChampionServiceImpl implements ChampionService {

    private final ChampionRepository championRepository;
    private final RiotApiService riotApiService;
    private final GameVersionRepository gameVersionRepository;

    @Override
    @Transactional
    public void updateChampionDataIfNecessary() {
        log.info("Riot Data Dragon 챔피언 데이터 업데이트 체크를 시작합니다.");

        // 1. 현재 DB에 챔피언 데이터의 존재 여부 확인
        boolean isDataExist = championRepository.count() > 0;
        String latestVersion = riotApiService.getLatestGameVersion();

        if (latestVersion == null) {
            log.error("최신 버전 정보를 가져올 수 없습니다. 챔피언 업데이트를 건너뜁니다.");
            return;
        }

        // 2. DB에 저장된 버전 정보 조회 및 비교 로직
        Optional<GameVersion> dbVersionOpt = gameVersionRepository.findTopByOrderByUpdateDateDesc();
        String dbVersion = dbVersionOpt.map(GameVersion::getVersion).orElse(null);

        // 챔피언 데이터가 없거나 (isDataExist = false) 또는 DB 버전과 최신 버전이 다를 경우 업데이트
        if (!isDataExist || !latestVersion.equals(dbVersion)) {
            log.warn("업데이트 필요 감지: DB 버전({}) vs 최신 버전({})", dbVersion, latestVersion);
            saveChampionData(latestVersion);
        } else {
            log.info("챔피언 데이터 버전({})이 최신입니다. 업데이트를 건너뜁니다.", latestVersion);
        }
    }

    /**
     * Data Dragon API를 호출하여 Champion 엔티티를 직접 파싱하고 DB에 저장합니다.
     */
    @Transactional
    public void saveChampionData(String version) {

        // RiotApiServiceImpl의 getLatestChampionDataAsChampionEntity() 호출 (Map<영문 Key, Champion> 반환 가정)
        Map<String, Champion> rawData = riotApiService.getLatestChampionDataAsChampionEntity();

        if (rawData == null || rawData.isEmpty()) {
            log.warn("Data Dragon API에서 유효한 챔피언 데이터를 가져오지 못했습니다.");
            return;
        }

        // 1. 기존 데이터 삭제
        championRepository.deleteAllInBatch();

        // 2. 챔피언 데이터 저장
        List<Champion> championsToSave = rawData.entrySet().stream()
                .map(entry -> {
                    String championKey = entry.getKey();
                    Champion champion = entry.getValue();

                    // ChampionKey 설정 (JSON에는 없으므로 Map의 Key를 사용)
                    champion.setChampionKey(championKey);

                    return champion;
                })
                .collect(Collectors.toList());

        championRepository.saveAll(championsToSave);

        // ⭐ 3. DB에 성공적으로 업데이트된 최신 버전 기록
        // NOTE: 이전 버전이 있으면 삭제하지 않고 새 버전 레코드를 추가하여 히스토리를 유지합니다.
        GameVersion newVersion = new GameVersion();
        newVersion.setVersion(version);
        newVersion.setUpdateDate(LocalDateTime.now());
        gameVersionRepository.save(newVersion);

        log.info("✅ 챔피언 데이터 초기화/업데이트 완료: {}개의 챔피언 데이터가 버전 {}으로 저장되었습니다.", championsToSave.size(), version);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Champion> findChampionByIdentifier(String identifier) {
        // 1. 이름(한글)으로 조회
        Optional<Champion> championByName = championRepository.findByName(identifier);
        if (championByName.isPresent()) {
            return championByName;
        }

        // 2. Champion Key(영문 고유 키)로 조회
        return championRepository.findByChampionKey(identifier);
    }
}
