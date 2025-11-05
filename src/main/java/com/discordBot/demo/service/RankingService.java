package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.enums.RankingCriterion;

import java.util.List;

public interface RankingService {

    /**
     * 특정 서버의 유저 통계를 조회하고 KDA 순으로 정렬된 랭킹 목록을 반환합니다.
     * @param serverId 디스코드 서버 ID
     * @param minGamesThreshold 랭킹에 포함되기 위한 최소 게임 수
     * @return KDA 순으로 정렬된 UserRankDto 목록
     */
    List<UserRankDto> getRanking(Long serverId, int minGamesThreshold, RankingCriterion criterion);
}