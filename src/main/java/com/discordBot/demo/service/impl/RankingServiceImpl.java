package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion; // Enum 임포트
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

    // (기존 getRankingByKDA 메서드는 유지한다고 가정)
    @Override
    public List<UserRankDto> getRankingByKDA(Long serverId, int minGamesThreshold) {
        // 이 메서드는 이제 getRanking(serverId, minGamesThreshold, RankingCriterion.KDA)를 호출하거나
        // 기본값인 승률을 따르도록 구현하면 됩니다.
        return getRanking(serverId, minGamesThreshold, RankingCriterion.WIN_RATE); // 승률을 기본값으로 사용
    }

    @Override
    public List<UserRankDto> getRanking(Long serverId, int minGamesThreshold, RankingCriterion criterion) {

        // 1. Comparator 동적 생성
        Comparator<UserRankDto> primaryComparator = switch (criterion) {
            case WIN_RATE -> Comparator.comparing(UserRankDto::getWinRate, Comparator.reverseOrder());
            case KDA -> Comparator.comparing(UserRankDto::getKda, Comparator.reverseOrder());
            case GAMES -> Comparator.comparing(UserRankDto::getTotalGames, Comparator.reverseOrder());
            // GPM, DPM, KP 등 추가 기준이 필요하다면 여기에 추가
            case GPM -> Comparator.comparing(UserRankDto::getGpm, Comparator.reverseOrder());
            case DPM -> Comparator.comparing(UserRankDto::getDpm, Comparator.reverseOrder());
            case KP -> Comparator.comparing(UserRankDto::getKillParticipation, Comparator.reverseOrder());
        };

        // 2. 모든 지표를 포괄하는 최종 Comparator 체인 생성
        Comparator<UserRankDto> finalComparator = primaryComparator
                // 2순위 이하: 1순위 기준이 동점일 때 사용 (KDA, GPM 등)
                // NOTE: 승률이 최우선일 때 KDA로, KDA가 최우선일 때 GPM으로 자연스럽게 넘어갑니다.
                .thenComparing(UserRankDto::getWinRate, Comparator.reverseOrder())
                .thenComparing(UserRankDto::getKda, Comparator.reverseOrder())
                .thenComparing(UserRankDto::getGpm, Comparator.reverseOrder())
                .thenComparing(UserRankDto::getDpm, Comparator.reverseOrder())
                .thenComparing(UserRankDto::getKillParticipation, Comparator.reverseOrder())
                // 최종 동점 처리: 총 게임 수
                .thenComparing(UserRankDto::getTotalGames, Comparator.reverseOrder());


        List<UserRankDto> rankedList = userServerStatsRepository.findAllByGuildServer_DiscordServerId(serverId)
                .stream()
                .map(UserRankDto::from)
                .filter(dto -> dto.getTotalGames() >= minGamesThreshold)
                .sorted(finalComparator) // ⭐ 동적으로 생성된 Comparator 사용
                .collect(Collectors.toList());

        log.info("랭킹 조회 완료: 서버 ID {} (기준: {})", serverId, criterion.getDisplayName());

        return rankedList;
    }
}