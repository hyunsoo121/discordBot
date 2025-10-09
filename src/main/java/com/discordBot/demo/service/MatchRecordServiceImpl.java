package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.*;
import com.discordBot.demo.domain.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional // 데이터 변경이 발생하는 메서드는 트랜잭션 처리
public class MatchRecordServiceImpl implements MatchRecordService { // 인터페이스 구현

    // 필요한 Repository 및 Service 주입
    private final MatchRecordRepository matchRecordRepository;
    private final GuildServerRepository guildServerRepository; // 가정
    private final UserRepository userRepository;
    private final LolAccountRepository lolAccountRepository;

    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        // 1. 유효성 검사 및 길드 서버 조회
        GuildServer guildServer = guildServerRepository.findById(matchDto.getServerId())
                .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 해당 서버 ID를 찾을 수 없습니다."));

        // 2. MatchRecord 엔티티 생성
        MatchRecord matchRecord = new MatchRecord();
        matchRecord.setGuildServer(guildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());

        // 3. PlayerStats 리스트 처리 및 양방향 관계 설정
        matchDto.getPlayerStatsList().stream()
                .forEach(playerDto -> {
                    // 3-A. 사용자 및 롤 계정 조회
                    User user = userRepository.findByDiscordUserId(playerDto.getDiscordUserId())
                            .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 등록되지 않은 디스코드 유저 ID입니다: " + playerDto.getDiscordUserId()));

                    LolAccount lolAccount = lolAccountRepository.findByGameNameAndTagLine(playerDto.getLolGameName(), playerDto.getLolTagLine())
                            .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 등록된 롤 계정을 찾을 수 없습니다: " + playerDto.getLolGameName() + "#" + playerDto.getLolTagLine()));

                    // 롤 계정 소유자 검증
                    if (!lolAccount.getUser().equals(user)) {
                        throw new IllegalArgumentException("❌ 오류: 롤 계정(" + lolAccount.getFullAccountName() + ")은 해당 디스코드 유저의 소유가 아닙니다.");
                    }

                    // 3-B. PlayerStats 생성 및 승패 설정
                    PlayerStats stats = new PlayerStats();
                    stats.setUser(user);
                    stats.setLolNickname(lolAccount);
                    stats.setTeam(playerDto.getTeam());
                    stats.setKills(playerDto.getKills());
                    stats.setDeaths(playerDto.getDeaths());
                    stats.setAssists(playerDto.getAssists());

                    // 승패 계산 및 설정
                    boolean isWin = playerDto.getTeam().equalsIgnoreCase(matchDto.getWinnerTeam());
                    stats.setIsWin(isWin);

                    // 3-C. 양방향 관계 설정 (MatchRecord 엔티티에 구현한 편의 메서드 사용)
                    // stats.setMatchRecord(matchRecord); 가 MatchRecord.addPlayerStats(stats) 내부에서 처리됨
                    matchRecord.addPlayerStats(stats);
                });

        // 4. MatchRecord 저장 (CascadeType.ALL에 의해 PlayerStats도 함께 저장됨)
        return matchRecordRepository.save(matchRecord);
    }
}