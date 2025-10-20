package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.*;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.MatchRecordRepository;
import com.discordBot.demo.service.MatchRecordService;
import com.discordBot.demo.service.ServerManagementService;
import com.discordBot.demo.service.UserServerStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MatchRecordServiceImpl implements MatchRecordService {

    private final MatchRecordRepository matchRecordRepository;
    private final ServerManagementService serverManagementService;
    private final LolAccountRepository lolAccountRepository;
    private final UserServerStatsService userServerStatsService;
    private final EntityManager em;


    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        Long discordServerId = matchDto.getServerId();

        // 중복 검사에 필요한 필드 추출
        int duration = matchDto.getGameDurationSeconds();
        int blueGold = matchDto.getBlueTotalGold();
        int redGold = matchDto.getRedTotalGold();

        // 서버 존재 확인 및 중복 검증 단계
        serverManagementService.findOrCreateGuildServer(discordServerId);

        // 매치 중복 검사 (경기 시간 + 팀별 골드)
        Optional<MatchRecord> existingMatch = matchRecordRepository
                .findByGameDurationSecondsAndBlueTotalGoldAndRedTotalGoldAndGuildServer_DiscordServerId(
                        duration,
                        blueGold,
                        redGold,
                        discordServerId
                );

        if (existingMatch.isPresent()) {
            throw new IllegalArgumentException(
                    "❌ 오류: 이 기록은 이미 등록된 것으로 보입니다. (동일 서버에서 동일 경기 시간, 동일 팀별 골드로 등록된 기록 존재)"
            );
        }

        // KP 계산을 위한 팀 총 킬 수 사전 계산
        int blueTeamKills = matchDto.getPlayerStatsList().stream()
                .filter(p -> "BLUE".equalsIgnoreCase(p.getTeam()))
                .mapToInt(PlayerStatsDto::getKills)
                .sum();

        int redTeamKills = matchDto.getPlayerStatsList().stream()
                .filter(p -> "RED".equalsIgnoreCase(p.getTeam()))
                .mapToInt(PlayerStatsDto::getKills)
                .sum();

        // 미등록 계정 검증 단계
        List<String> unregisteredAccounts = new ArrayList<>();
        for (PlayerStatsDto playerDto : matchDto.getPlayerStatsList()) {
            String gameName = playerDto.getLolGameName();
            String tagLine = StringUtils.hasText(playerDto.getLolTagLine()) ? playerDto.getLolTagLine() : "";

            Optional<LolAccount> lolAccountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                    gameName,
                    tagLine,
                    discordServerId
            );

            if (lolAccountOpt.isEmpty()) {
                unregisteredAccounts.add(gameName + "#" + tagLine);
            }
        }

        // 검증 결과 확인 및 예외 발생
        if (!unregisteredAccounts.isEmpty()) {
            String missingList = String.join(", ", unregisteredAccounts);

            throw new IllegalArgumentException(
                    "❌ 오류: 이 서버에 등록되지 않은 롤 계정이 포함되어 있습니다. 먼저 `/register` 명령어로 해당 계정을 등록해 주세요.\n\n" +
                            "**[미등록 계정 목록]**\n" + missingList
            );
        }

        // MatchRecord 엔티티 생성 및 필드 설정
        MatchRecord matchRecord = new MatchRecord();
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, discordServerId);

        matchRecord.setGuildServer(proxyGuildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());
        matchRecord.setMatchDate(LocalDateTime.now());

        // 중복 방지 및 통계 필드 설정
        matchRecord.setGameDurationSeconds(duration);
        matchRecord.setBlueTotalGold(blueGold);
        matchRecord.setRedTotalGold(redGold);

        // PlayerStats 및 UserServerStats 저장
        matchDto.getPlayerStatsList().forEach(playerDto -> {

            String gameName = playerDto.getLolGameName();
            String tagLine = StringUtils.hasText(playerDto.getLolTagLine()) ? playerDto.getLolTagLine() : "";

            LolAccount lolAccount = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                    gameName, tagLine, discordServerId).get();

            User user = lolAccount.getUser();

            PlayerStats stats = new PlayerStats();
            stats.setUser(user);
            stats.setLolNickname(lolAccount);
            stats.setTeam(playerDto.getTeam());
            stats.setKills(playerDto.getKills());
            stats.setDeaths(playerDto.getDeaths());
            stats.setAssists(playerDto.getAssists());

            boolean isWin = playerDto.getTeam().equalsIgnoreCase(matchDto.getWinnerTeam());
            stats.setIsWin(isWin);

            matchRecord.addPlayerStats(stats);

            // 현재 플레이어 팀의 총 킬 수 결정 (KP 분모)
            int playerTeamKills = playerDto.getTeam().equalsIgnoreCase("BLUE") ? blueTeamKills : redTeamKills;

            // 누적 통계 업데이트 로직 호출 (새로운 인자 전달)
            if (user != null) {
                userServerStatsService.updateStatsAfterMatch(
                        user.getDiscordUserId(),
                        discordServerId,
                        playerDto,
                        isWin,
                        duration,
                        playerTeamKills
                );
            }
        });

        // MatchRecord 저장
        return matchRecordRepository.save(matchRecord);
    }
}