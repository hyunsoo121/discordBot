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
    private final UserServerStatsService userServerStatsService; // ⭐ 누적 통계 서비스
    private final EntityManager em;


    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        Long discordServerId = matchDto.getServerId();

        // 서버 존재 확인 (없으면 생성)
        serverManagementService.findOrCreateGuildServer(discordServerId);

        // 1. 미등록 계정 검증 단계
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

        // 2. 검증 결과 확인 및 예외 발생
        if (!unregisteredAccounts.isEmpty()) {
            String missingList = String.join(", ", unregisteredAccounts);

            throw new IllegalArgumentException(
                    "❌ 오류: 이 서버에 등록되지 않은 롤 계정이 포함되어 있습니다. 먼저 `/register` 명령어로 해당 계정을 등록해 주세요.\n\n" +
                            "**[미등록 계정 목록]**\n" + missingList
            );
        }

        // 3. MatchRecord 생성 및 PlayerStats/UserServerStats 저장
        MatchRecord matchRecord = new MatchRecord();
        // 성능 최적화를 위해 getReference 사용
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, discordServerId);

        matchRecord.setGuildServer(proxyGuildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());

        matchDto.getPlayerStatsList().forEach(playerDto -> {

            String gameName = playerDto.getLolGameName();
            String tagLine = StringUtils.hasText(playerDto.getLolTagLine()) ? playerDto.getLolTagLine() : "";

            // 계정 재조회 (검증 단계에서 존재 확인했으므로 .get() 사용)
            LolAccount lolAccount = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                    gameName, tagLine, discordServerId).get();

            // 롤 계정에 연결된 디스코드 유저 (없으면 null)
            User user = lolAccount.getUser();

            // PlayerStats 엔티티 생성 및 설정
            PlayerStats stats = new PlayerStats();
            stats.setUser(user);
            stats.setLolNickname(lolAccount);
            stats.setTeam(playerDto.getTeam());
            stats.setKills(playerDto.getKills());
            stats.setDeaths(playerDto.getDeaths());
            stats.setAssists(playerDto.getAssists());

            boolean isWin = playerDto.getTeam().equalsIgnoreCase(matchDto.getWinnerTeam());
            stats.setIsWin(isWin);

            // MatchRecord와 PlayerStats 연결
            matchRecord.addPlayerStats(stats);

            // ⭐ ⭐ 4. 누적 통계 업데이트 로직 호출 (핵심) ⭐ ⭐
            // user가 null이 아니어야 통계가 유효합니다.
            if (user != null) {
                userServerStatsService.updateStatsAfterMatch(
                        user.getDiscordUserId(),
                        discordServerId,
                        playerDto,
                        isWin
                );
            }
        });

        // 5. MatchRecord 저장 (PlayerStats도 Cascade로 저장)
        return matchRecordRepository.save(matchRecord);
    }
}