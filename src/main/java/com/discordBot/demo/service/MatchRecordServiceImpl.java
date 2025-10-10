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
    private final EntityManager em;


    @Override
    public MatchRecord registerMatch(MatchRegistrationDto matchDto) {

        Long discordServerId = matchDto.getServerId();

        serverManagementService.findOrCreateGuildServer(discordServerId);

        // 1. 미등록 계정 목록을 저장할 리스트 초기화
        List<String> unregisteredAccounts = new ArrayList<>();

        // 2. 모든 PlayerStats에 대해 롤 계정 등록 여부 검증
        for (PlayerStatsDto playerDto : matchDto.getPlayerStatsList()) {

            String gameName = playerDto.getLolGameName();
            String tagLine = playerDto.getLolTagLine();

            // TagLine이 없으면 빈 문자열로 표준화 (DB 검색 기준 통일)
            if (!StringUtils.hasText(tagLine)) {
                tagLine = "";
            }

            // DB에서 롤 계정 찾기
            Optional<LolAccount> lolAccountOpt = lolAccountRepository.findByGameNameAndTagLine(gameName, tagLine);

            if (lolAccountOpt.isEmpty()) {
                // DB에 없는 계정이 발견됨: 미등록 리스트에 추가
                unregisteredAccounts.add(gameName + "#" + tagLine);
            }
        }

        // 3. 검증 결과 확인: 미등록 계정이 하나라도 있다면 트랜잭션 취소
        if (!unregisteredAccounts.isEmpty()) {
            String missingList = String.join(", ", unregisteredAccounts);

            // 사용자에게 오류 메시지와 미등록 계정 목록을 보여줍니다.
            throw new IllegalArgumentException(
                    "❌ 오류: DB에 등록되지 않은 롤 계정이 포함되어 있습니다. 먼저 `/admin-register` 명령어로 해당 계정을 등록해 주세요.\n\n" +
                            "**[미등록 계정 목록]**\n" + missingList
            );
            // 이 예외는 SlashCommandListener로 전달되어 트랜잭션을 롤백하고 사용자에게 메시지를 보여줍니다.
        }

        // 4. 검증 완료: 이제 안전하게 MatchRecord 생성 및 PlayerStats 저장
        MatchRecord matchRecord = new MatchRecord();
        GuildServer proxyGuildServer = em.getReference(GuildServer.class, discordServerId);

        matchRecord.setGuildServer(proxyGuildServer);
        matchRecord.setWinnerTeam(matchDto.getWinnerTeam());

        matchDto.getPlayerStatsList().forEach(playerDto -> {
            // 검증이 완료되었으므로, lolAccountOpt는 반드시 Present합니다.
            String gameName = playerDto.getLolGameName();
            String tagLine = playerDto.getLolTagLine();
            if (!StringUtils.hasText(tagLine)) tagLine = "";

            // DB에서 계정 정보를 다시 가져옴
            LolAccount lolAccount = lolAccountRepository.findByGameNameAndTagLine(gameName, tagLine).get();

            // 롤 계정에 연결된 디스코드 유저 (없으면 null)
            User user = lolAccount.getUser();

            // PlayerStats 생성 및 설정
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

        // 5. MatchRecord 저장
        return matchRecordRepository.save(matchRecord);
    }

    // 이 외의 linkExistingAccount 등의 메서드는 그대로 유지됩니다.
}
