package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
import com.discordBot.demo.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RankingServiceImpl implements RankingService {

    private final UserServerStatsRepository userServerStatsRepository;

    @Override
    public List<UserRankDto> getRankingByKDA(Long serverId, int minGamesThreshold) {

        // 특정 서버의 모든 UserServerStats 조회
        List<UserRankDto> rankedList = userServerStatsRepository.findAllByGuildServer_DiscordServerId(serverId)
                .stream()
                .map(UserRankDto::from) // UserRankDto.from()에서 5가지 지표 모두 계산

                // 2. 최소 게임 수 필터링
                .filter(dto -> dto.getTotalGames() >= minGamesThreshold)

                // ⭐ 정렬 기준 적용 (KDA > GPM > KP > DPM > 승률 순으로 가정)
                .sorted(Comparator
                        // 1순위: KDA (킬/데스/어시스트 효율)
                        .comparing(UserRankDto::getKda, Comparator.reverseOrder())
                        // 2순위: GPM (경제력 확보 효율)
                        .thenComparing(UserRankDto::getGpm, Comparator.reverseOrder())
                        // 3순위: Kill Participation (팀 전투 참여율)
                        .thenComparing(UserRankDto::getKillParticipation, Comparator.reverseOrder())
                        // 4순위: DPM (피해량 효율)
                        .thenComparing(UserRankDto::getDpm, Comparator.reverseOrder())
                        // 5순위: 승률 및 게임 수 (최종 동점 처리)
                        .thenComparing(UserRankDto::getWinRate, Comparator.reverseOrder())
                        .thenComparing(UserRankDto::getTotalGames, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        log.info("랭킹 조회 완료: 서버 ID {}에서 총 {}명의 플레이어가 랭킹에 포함되었습니다.", serverId, rankedList.size());

        return rankedList;
    }
}