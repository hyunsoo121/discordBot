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

                // ⭐ 정렬 기준 적용 (승률 > GPM > KP > DPM > 승률 순으로 가정)
                .sorted(Comparator
                        // 1순위: 승률 (Win Rate) - 내림차순
                        .comparing(UserRankDto::getWinRate, Comparator.reverseOrder())

                        // 2순위: KDA - 내림차순 (승률 동점 시)
                        .thenComparing(UserRankDto::getKda, Comparator.reverseOrder())

                        // 3순위 그룹: GPM, KP, DPM (KDA 동점 시)
                        .thenComparing(UserRankDto::getGpm, Comparator.reverseOrder())
                        .thenComparing(UserRankDto::getKillParticipation, Comparator.reverseOrder())
                        .thenComparing(UserRankDto::getDpm, Comparator.reverseOrder())

                        // 4순위: 총 게임 수 (Total Games) - 최종 동점 처리
                        .thenComparing(UserRankDto::getTotalGames, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        return rankedList;
    }
}