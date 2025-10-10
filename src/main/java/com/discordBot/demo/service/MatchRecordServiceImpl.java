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
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final RiotApiService riotApiService;


    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        Long discordServerId = matchDto.getServerId(); // Discord ID (DB PK)

        serverManagementService.findOrCreateGuildServer(discordServerId);

        MatchRecord matchRecord = new MatchRecord();
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, discordServerId);

        matchRecord.setGuildServer(proxyGuildServer); // Proxy 객체를 외래 키 설정에 사용
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());

        matchDto.getPlayerStatsList().stream()
                .forEach(playerDto -> {

                    User user = userRepository.findByDiscordUserId(playerDto.getDiscordUserId())
                            .orElse(null);

                    String rawGameName = playerDto.getLolGameName();

                    // ⭐⭐⭐ 핵심 수정: GameName 정규화 (모든 공백 및 앞뒤 공백 제거) ⭐⭐⭐
                    String gameName = rawGameName.trim().replaceAll("\\s+", "");

                    String tagLine = playerDto.getLolTagLine();

                    // 3-B. LolAccount 찾기 로직
                    Optional<LolAccount> lolAccountOpt = Optional.empty();

                    if (!StringUtils.hasText(tagLine)) {
                        tagLine = "";
                    }

                    if (StringUtils.hasText(tagLine)) {
                        // 1순위: TagLine이 있다면 정확한 조합으로 찾습니다.
                        // (정규화된 gameName 사용)
                        lolAccountOpt = lolAccountRepository.findByGameNameAndTagLine(gameName, tagLine);

                    } else {
                        // 2순위: TagLine이 없다면, GameName이 같은 모든 계정 중 연결 가능한 것을 찾습니다.
                        // (정규화된 gameName 사용)
                        List<LolAccount> accountsWithSameGameName = lolAccountRepository.findByGameName(gameName);

                        // 최우선 순위: TagLine이 채워진 계정 (register된 계정)을 찾습니다.
                        lolAccountOpt = accountsWithSameGameName.stream()
                                .filter(account -> StringUtils.hasText(account.getTagLine()))
                                .findFirst();

                        if (lolAccountOpt.isEmpty()) {
                            // TagLine이 채워진 계정이 없다면, TagLine이 없는 (매치 등록으로 생성된) 임시 계정을 찾습니다.
                            lolAccountOpt = accountsWithSameGameName.stream()
                                    .filter(account -> !StringUtils.hasText(account.getTagLine()))
                                    .findFirst();
                        }
                    }
                    // 3-B 끝

                    // 3-C. LolAccount 찾기 또는 생성
                    final String finalTagLine = tagLine;
                    LolAccount lolAccount;

                    if (lolAccountOpt.isPresent()) {
                        // 계정이 DB에 존재함: 기존 계정 사용
                        lolAccount = lolAccountOpt.get();
                    } else {
                        // 계정이 DB에 없음: 새로운 계정 생성 및 저장

                        LolAccount newAccount = new LolAccount();
                        newAccount.setGameName(gameName); // 정규화된 이름으로 저장
                        newAccount.setTagLine(finalTagLine);

                        newAccount.setUser(user);

                        lolAccount = lolAccountRepository.save(newAccount);
                    }


                    // 3-D. 소유자 검증 (제거됨)

                    // 3-E. PlayerStats 생성 및 설정
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
