package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.*;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.MatchRecordRepository;
import com.discordBot.demo.service.MatchRecordService;
import com.discordBot.demo.service.ServerManagementService;
import com.discordBot.demo.service.UserServerStatsService;
import com.discordBot.demo.service.ChampionStatsService;
import com.discordBot.demo.service.LineStatsService; // ⭐ LineStatsService 임포트
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
    private final ChampionStatsService championStatsService;
    private final LineStatsService lineStatsService; // ⭐ LineStatsService 주입
    private final EntityManager em;


    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        Long discordServerId = matchDto.getServerId();

        // 중복 검사에 필요한 필드 추출
        int duration = matchDto.getGameDurationSeconds();
        int blueGold = matchDto.getBlueTotalGold();
        int redGold = matchDto.getRedTotalGold();

        // 1. 서버 존재 확인 및 중복 검증 단계 (로직 유지)
        serverManagementService.findOrCreateGuildServer(discordServerId);

        // 1-1. 매치 중복 검사 (로직 유지)
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

        // 1-2. KP 계산을 위한 팀 총 킬 수 사전 계산 (로직 유지)
        int blueTeamKills = matchDto.getPlayerStatsList().stream()
                .filter(p -> "BLUE".equalsIgnoreCase(p.getTeam()))
                .mapToInt(PlayerStatsDto::getKills)
                .sum();

        int redTeamKills = matchDto.getPlayerStatsList().stream()
                .filter(p -> "RED".equalsIgnoreCase(p.getTeam()))
                .mapToInt(PlayerStatsDto::getKills)
                .sum();

        // 2. 미등록 계정 검증 단계 (로직 유지)
        List<String> unregisteredAccounts = new ArrayList<>();
        for (PlayerStatsDto playerDto : matchDto.getPlayerStatsList()) {
            String gameName = playerDto.getLolGameName();
            String tagLine = StringUtils.hasText(playerDto.getLolTagLine()) ? playerDto.getLolTagLine() : "";

            Optional<LolAccount> lolAccountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                    gameName, tagLine, discordServerId
            );

            if (lolAccountOpt.isEmpty()) {
                unregisteredAccounts.add(gameName + "#" + tagLine);
            }
        }

        if (!unregisteredAccounts.isEmpty()) {
            String missingList = String.join(", ", unregisteredAccounts);
            throw new IllegalArgumentException(
                    "❌ 오류: 이 서버에 등록되지 않은 롤 계정이 포함되어 있습니다. 먼저 `/register` 명령어로 해당 계정을 등록해 주세요.\n\n" +
                            "**[미등록 계정 목록]**\n" + missingList
            );
        }

        // 3. MatchRecord 엔티티 생성 및 필드 설정 (로직 유지)
        MatchRecord matchRecord = new MatchRecord();
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, discordServerId);

        matchRecord.setGuildServer(proxyGuildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());
        matchRecord.setMatchDate(LocalDateTime.now());
        matchRecord.setGameDurationSeconds(duration);
        matchRecord.setBlueTotalGold(blueGold);
        matchRecord.setRedTotalGold(redGold);

        // 4. PlayerStats 및 통계 저장
        matchDto.getPlayerStatsList().forEach(playerDto -> {

            String gameName = playerDto.getLolGameName();
            String tagLine = StringUtils.hasText(playerDto.getLolTagLine()) ? playerDto.getLolTagLine() : "";

            LolAccount lolAccount = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                    gameName, tagLine, discordServerId).get();

            User user = lolAccount.getUser();
            int playerTeamKills = playerDto.getTeam().equalsIgnoreCase("BLUE") ? blueTeamKills : redTeamKills;

            // PlayerStats 엔티티 생성 및 설정 (챔피언, 라인 FK 설정 로직 추가 필요)
            PlayerStats stats = new PlayerStats();
            stats.setUser(user);
            stats.setLolNickname(lolAccount);
            stats.setTeam(playerDto.getTeam());
            stats.setKills(playerDto.getKills());
            stats.setDeaths(playerDto.getDeaths());
            stats.setAssists(playerDto.getAssists());

            // ⭐ DPM/GPM 필드 설정 추가
            stats.setTotalGold(playerDto.getTotalGold());
            stats.setTotalDamage(playerDto.getTotalDamage());
            stats.setDurationSeconds(playerDto.getDurationSeconds());


            boolean isWin = playerDto.getTeam().equalsIgnoreCase(matchDto.getWinnerTeam());
            stats.setIsWin(isWin);

            // TODO: stats.setChampion()과 stats.setLine() 호출 필요

            matchRecord.addPlayerStats(stats);

            // ⭐ 4-1. UserServerStats 업데이트 (전체 통계)
            if (user != null) {
                userServerStatsService.updateStatsAfterMatch(
                        user.getDiscordUserId(),
                        discordServerId,
                        playerDto,
                        isWin,
                        (long) duration,
                        playerTeamKills
                );

                // ⭐⭐ 4-2. ChampionStats 업데이트 (챔피언별 통계)
                championStatsService.updateChampionStats(
                        playerDto.getChampionName(),
                        user.getDiscordUserId(),
                        discordServerId,
                        playerDto,
                        isWin,
                        (long) duration,
                        playerTeamKills
                );

                // ⭐⭐ 4-3. LineStats 업데이트 (라인별 통계)
                lineStatsService.updateLineStats(
                        user.getDiscordUserId(),
                        discordServerId,
                        playerDto,
                        isWin,
                        (long) duration,
                        playerTeamKills
                );
            }
        });

        // 5. MatchRecord 저장
        return matchRecordRepository.save(matchRecord);
    }
}