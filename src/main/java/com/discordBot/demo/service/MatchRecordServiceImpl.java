package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.MatchRegistrationDto;
import com.discordBot.demo.domain.dto.PlayerStatsDto;
import com.discordBot.demo.domain.entity.GuildServer;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.MatchRecord;
import com.discordBot.demo.domain.entity.PlayerStats;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.MatchRecordRepository;
import com.discordBot.demo.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MatchRecordServiceImpl implements MatchRecordService {

    private final MatchRecordRepository matchRecordRepository;
    private final ServerManagementService serverManagementService;
    private final UserRepository userRepository;
    private final LolAccountRepository lolAccountRepository;
    private final EntityManager em;

    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        Long serverId = matchDto.getServerId();

        // 1. GuildServer의 존재를 별도 트랜잭션에서 보장하고, 실제 객체를 가져옵니다.
        // 이 객체는 DB에서 로드되거나 새로 생성되었으므로, 실제 PK(id)를 가지고 있습니다.
        GuildServer persistedGuildServer = serverManagementService.findOrCreateGuildServer(serverId);

        // 2. MatchRecord 엔티티 생성
        MatchRecord matchRecord = new MatchRecord();

        // ⭐⭐ 핵심 수정: Discord ID 대신 DB가 생성한 실제 PK(persistedGuildServer.getId())를 사용합니다. ⭐⭐
        // 이 PK를 getReference()에 사용하여 충돌을 방지합니다.
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, persistedGuildServer.getId());

        matchRecord.setGuildServer(proxyGuildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());

        // 3. PlayerStats 리스트 처리 및 양방향 관계 설정
        matchDto.getPlayerStatsList().stream()
                .forEach(playerDto -> {

                    // 3-A. User 찾기 (없으면 NULL로 둡니다.)
                    User user = userRepository.findByDiscordUserId(playerDto.getDiscordUserId())
                            .orElse(null);

                    // 3-B. LolAccount 찾기 또는 생성
                    Optional<LolAccount> lolAccountOpt = lolAccountRepository.findByGameNameAndTagLine(playerDto.getLolGameName(), playerDto.getLolTagLine());

                    LolAccount lolAccount = lolAccountOpt
                            .orElseGet(() -> {
                                log.info("새로운 롤 계정 등록 시도: {}{}", playerDto.getLolGameName(), playerDto.getLolTagLine());

                                LolAccount newAccount = new LolAccount();
                                newAccount.setGameName(playerDto.getLolGameName());
                                newAccount.setTagLine(playerDto.getLolTagLine());

                                newAccount.setUser(user);

                                return lolAccountRepository.save(newAccount);
                            });

                    // 3-C. 소유자 검증
                    if (lolAccountOpt.isPresent()) {
                        User existingUser = lolAccount.getUser();

                        if (existingUser != null && !existingUser.equals(user)) {
                            throw new IllegalArgumentException("❌ 오류: 롤 계정(" + lolAccount.getFullAccountName() + ")은 이미 다른 디스코드 유저에게 등록되어 있습니다.");
                        }
                    }

                    // 3-D. PlayerStats 생성 및 설정
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
                });

        // 4. MatchRecord 저장
        return matchRecordRepository.save(matchRecord);
    }
}
