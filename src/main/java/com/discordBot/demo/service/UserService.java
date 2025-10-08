package com.discordBot.demo.service;


import org.springframework.transaction.annotation.Transactional;

public interface UserService {
    /**
     * 롤 닉네임을 검증하고 데이터베이스에 등록합니다.
     * @param discordUserId 닉네임을 등록하는 디스코드 사용자 ID
     * @param gameName 롤 게임 이름 (닉네임)
     * @param tagLine 롤 태그 라인 (예: KR1)
     * @return 작업 결과 메시지
     */
    @Transactional
    String registerLolNickname(Long discordUserId, String gameName, String tagLine);
}
