package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;

public interface ImageAnalysisService {

    /**
     * 이미지 URL을 받아 Gemini LLM을 통해 데이터를 분석하고 구조화된 DTO를 반환합니다.
     * @param imageUrl 분석할 이미지의 URL
     * @param winnerTeam 사용자가 입력한 승리팀
     * @param serverId 경기가 진행된 서버 ID
     * @return 분석 결과를 담은 MatchRegistrationDto
     * @throws Exception 분석 실패 또는 Gemini API 호출 오류 시
     */
    MatchRegistrationDto analyzeAndStructureData(String imageUrl, String winnerTeam, Long serverId) throws Exception;
}