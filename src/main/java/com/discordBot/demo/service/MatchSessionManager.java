package com.discordBot.demo.service; // service 하위 패키지에 포함

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class MatchSessionManager {

    private final Map<String, MatchSession> activeMatchSessions = new ConcurrentHashMap<>();
    private static final int TOTAL_PLAYERS = 10;

    // ⭐ 세션 내부 상태 클래스
    public static class MatchSession {
        public Long serverId;
        public String winnerTeam;
        public List<PlayerStatsDto> playerStatsList = new ArrayList<>();
        public int nextPlayerIndex = 0; // 0부터 9까지
    }

    /** 세션을 시작하고 초기 상태를 저장합니다. */
    public MatchSession startSession(String initiatorId, Long serverId, String winnerTeam) {
        if (activeMatchSessions.containsKey(initiatorId)) {
            throw new IllegalStateException("이미 진행 중인 세션이 있습니다.");
        }
        MatchSession session = new MatchSession();
        session.serverId = serverId;
        session.winnerTeam = winnerTeam;
        activeMatchSessions.put(initiatorId, session);
        return session;
    }

    /** 세션에 선수 정보를 추가하고 다음 순서를 반환합니다. */
    public int addPlayerStats(String initiatorId, PlayerStatsDto dto) {
        MatchSession session = getSession(initiatorId);

        // 팀 할당 (0~4: RED, 5~9: BLUE)
        dto.setTeam(session.nextPlayerIndex < (TOTAL_PLAYERS / 2) ? "RED" : "BLUE");

        session.playerStatsList.add(dto);
        session.nextPlayerIndex++;

        return session.nextPlayerIndex;
    }

    /** 세션을 종료하고 최종 MatchRegistrationDto를 조립합니다. */
    public MatchRegistrationDto assembleAndFinishSession(String initiatorId) {
        MatchSession session = activeMatchSessions.remove(initiatorId);
        if (session == null) {
            throw new IllegalStateException("세션을 찾을 수 없어 최종 등록에 실패했습니다.");
        }

        MatchRegistrationDto finalDto = new MatchRegistrationDto();
        finalDto.setServerId(session.serverId);
        finalDto.setWinnerTeam(session.winnerTeam);
        finalDto.setPlayerStatsList(session.playerStatsList);

        return finalDto;
    }

    /** 진행 중인 세션을 조회합니다. (없으면 Exception 발생) */
    public MatchSession getSession(String initiatorId) {
        MatchSession session = activeMatchSessions.get(initiatorId);
        if (session == null) {
            throw new IllegalStateException("세션이 만료되었거나 존재하지 않습니다.");
        }
        return session;
    }

    /** 세션을 강제로 제거합니다. */
    public void removeSession(String initiatorId) {
        activeMatchSessions.remove(initiatorId);
    }
}