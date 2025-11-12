package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.LineRankDto;
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

    /**
     * 특정 라인에 대한 라인별 랭킹을 반환합니다.
     * @param serverId 서버 ID
     * @param lineId 라인 엔티티 ID
     * @param minGamesThreshold 랭킹 포함을 위한 최소 게임 수
     * @param criterion 정렬 기준
     * @return LineRankDto 목록
     */
    List<LineRankDto> getLineRanking(Long serverId, Long lineId, int minGamesThreshold, RankingCriterion criterion);
}