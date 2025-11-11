package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;

public interface TemporaryMatchStorageService {

    /**
     * 분석된 DTO를 임시로 저장하고, 저장된 ID를 반환합니다.
     */
    Long saveTemporaryMatch(MatchRegistrationDto dto);

    /**
     * 임시 저장된 DTO를 ID로 불러옵니다.
     */
    MatchRegistrationDto getTemporaryMatch(Long id);

    /**
     * 임시 저장된 DTO를 ID로 삭제합니다 (등록 완료 또는 취소 시).
     */
    void removeTemporaryMatch(Long id);

    /**
     * 임시 저장된 DTO를 업데이트합니다. (모달 제출 후 사용)
     */
    void updateTemporaryMatch(Long id, MatchRegistrationDto updatedDto);
}