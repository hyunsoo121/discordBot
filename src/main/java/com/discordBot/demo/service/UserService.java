package com.discordBot.demo.service;

import com.discordBot.demo.domain.entity.LolAccount;
import java.util.List;

public interface UserService {

    /**
     * Riot ID를 디스코드 유저에게 연결하거나 새로운 롤 계정을 등록합니다.
     */
    String registerLolNickname(Long discordUserId, String gameName, String tagLine, Long discordServerId);

    /**
     * ⭐ 신규 추가: DB에 존재하는 LolAccount (accountId)에 Discord User를 연결합니다.
     * @param discordUserId 연결을 요청한 디스코드 유저 ID
     * @param lolAccountId 연결할 롤 계정의 DB ID
     * @return 처리 결과 메시지
     */
    String linkExistingAccount(Long discordUserId, Long lolAccountId);
}
