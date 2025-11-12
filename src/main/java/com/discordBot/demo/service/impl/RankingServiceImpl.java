package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.LineRankDto;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.repository.LineStatsRepository;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
import com.discordBot.demo.service.RankingService;
import com.discordBot.demo.domain.enums.RankingCriterion;
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
    private final LineStatsRepository lineStatsRepository;

    @Override
    public List<UserRankDto> getRanking(Long serverId, int minGamesThreshold, RankingCriterion criterion) {

        Comparator<UserRankDto> primaryComparator = switch (criterion) {
            case WINRATE -> Comparator.comparing(UserRankDto::getWinRate, Comparator.reverseOrder());
            case KDA -> Comparator.comparing(UserRankDto::getKda, Comparator.reverseOrder());
            case GAMES -> Comparator.comparing(UserRankDto::getTotalGames, Comparator.reverseOrder());

            case GPM -> Comparator.comparing(UserRankDto::getGpm, Comparator.reverseOrder());
            case DPM -> Comparator.comparing(UserRankDto::getDpm, Comparator.reverseOrder());
            case KP -> Comparator.comparing(UserRankDto::getKillParticipation, Comparator.reverseOrder());
        };

        Comparator<UserRankDto> finalComparator = primaryComparator
                // 2순위 이하: 1순위 기준이 동점일 때 사용
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
                .sorted(finalComparator)
                .collect(Collectors.toList());

        log.info("랭킹 조회 완료: 서버 ID {} (기준: {})", serverId, criterion.getDisplayName());

        return rankedList;
    }

    @Override
    public List<LineRankDto> getLineRanking(Long serverId, Long lineId, int minGamesThreshold, RankingCriterion criterion) {

        Comparator<LineRankDto> primaryComparator = switch (criterion) {
            case WINRATE -> Comparator.comparing(LineRankDto::getWinRate, Comparator.reverseOrder());
            case KDA -> Comparator.comparing(LineRankDto::getKda, Comparator.reverseOrder());
            case GAMES -> Comparator.comparing(LineRankDto::getTotalGames, Comparator.reverseOrder());

            case GPM -> Comparator.comparing(LineRankDto::getGpm, Comparator.reverseOrder());
            case DPM -> Comparator.comparing(LineRankDto::getDpm, Comparator.reverseOrder());
            case KP -> Comparator.comparing(LineRankDto::getKillParticipation, Comparator.reverseOrder());
        };

        // ⭐ 중복된 fully-qualified names 제거 및 단순화
        Comparator<LineRankDto> finalComparator = primaryComparator
                .thenComparing(LineRankDto::getWinRate, Comparator.reverseOrder())
                .thenComparing(LineRankDto::getKda, Comparator.reverseOrder())
                .thenComparing(LineRankDto::getGpm, Comparator.reverseOrder())
                .thenComparing(LineRankDto::getDpm, Comparator.reverseOrder())
                .thenComparing(LineRankDto::getKillParticipation, Comparator.reverseOrder())
                .thenComparing(LineRankDto::getTotalGames, Comparator.reverseOrder());

        // ⭐ 중복된 fully-qualified names 제거 및 단순화
        List<LineRankDto> rankedList = lineStatsRepository
                .findAllByGuildServer_DiscordServerIdAndLine_LineId(serverId, lineId)
                .stream()
                .map(LineRankDto::from)
                .filter(dto -> dto.getTotalGames() >= minGamesThreshold)
                .sorted(finalComparator)
                .collect(Collectors.toList());

        log.info("라인별 랭킹 조회 완료: 서버 ID {} 라인 {} (기준: {})", serverId, lineId, criterion.getDisplayName());

        return rankedList;
    }
}