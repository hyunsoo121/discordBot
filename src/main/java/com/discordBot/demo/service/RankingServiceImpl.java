package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.entity.UserServerStats;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ⭐ 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j // ⭐ 추가
public class RankingServiceImpl implements RankingService {

    private final UserServerStatsRepository userServerStatsRepository;

    @Override
    public List<UserRankDto> getRankingByKDA(Long serverId, int minGamesThreshold) {

        // 1. 특정 서버의 모든 UserServerStats 조회
        List<UserServerStats> allStats = userServerStatsRepository.findAllByGuildServer_DiscordServerId(serverId);

        List<UserRankDto> rankedList = allStats.stream()
                .map(UserRankDto::from)
                .filter(dto -> dto.getTotalGames() >= minGamesThreshold)
                .sorted(Comparator.comparing(UserRankDto::getKda).reversed())
                .collect(Collectors.toList());

        return rankedList;
    }
}