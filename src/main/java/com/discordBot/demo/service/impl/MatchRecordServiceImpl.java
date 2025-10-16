package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.MatchRecord;
import com.discordBot.demo.domain.entity.PlayerStats;
import com.discordBot.demo.domain.entity.User;
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

        int duration = matchDto.getGameDurationSeconds();
        int blueGold = matchDto.getBlueTotalGold();
        int redGold = matchDto.getRedTotalGold();

        serverManagementService.findOrCreateGuildServer(discordServerId);

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

        if (!unregisteredAccounts.isEmpty()) {
            String missingList = String.join(", ", unregisteredAccounts);

            throw new IllegalArgumentException(
                    "❌ 오류: 이 서버에 등록되지 않은 롤 계정이 포함되어 있습니다. 먼저 `/register` 명령어로 해당 계정을 등록해 주세요.\n\n" +
                            "**[미등록 계정 목록]**\n" + missingList
            );
        }

        MatchRecord matchRecord = new MatchRecord();
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, discordServerId);

        matchRecord.setGuildServer(proxyGuildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());
        matchRecord.setMatchDate(LocalDateTime.now());

        // ⭐⭐ DTO에서 추출한 필드를 엔티티에 설정
        matchRecord.setGameDurationSeconds(duration);
        matchRecord.setBlueTotalGold(blueGold);
        matchRecord.setRedTotalGold(redGold);
        // ⭐⭐ 엔티티 필드 설정 끝

        // 4. PlayerStats 및 UserServerStats 저장
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

            // 누적 통계 업데이트 로직 호출
            if (user != null) {
                userServerStatsService.updateStatsAfterMatch(
                        user.getDiscordUserId(),
                        discordServerId,
                        playerDto,
                        isWin
                );
            }
        });

        // 5. MatchRecord 저장
        return matchRecordRepository.save(matchRecord);
    }
}