package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.ChampionSearchDto;
import com.discordBot.demo.domain.dto.UserSearchDto;
import com.discordBot.demo.domain.dto.UserRankDto;
import com.discordBot.demo.domain.entity.ChampionStats;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.entity.UserServerStats;
import com.discordBot.demo.domain.repository.ChampionStatsRepository;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.domain.repository.UserServerStatsRepository;
import com.discordBot.demo.service.UserSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserSearchServiceImpl implements UserSearchService {

    private final UserRepository userRepository;
    private final UserServerStatsRepository userServerStatsRepository;
    private final ChampionStatsRepository championStatsRepository;
    private final LolAccountRepository lolAccountRepository;

    @Override
    public Optional<UserSearchDto> searchUserInternalStatsByRiotId(String summonerName, String tagLine, Long serverId) {

        // 1. LolAccount Repository를 통해 Riot ID에 연결된 LolAccount 조회 (서버 ID 포함)
        Optional<LolAccount> lolAccountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                summonerName,
                tagLine,
                serverId
        );

        if (lolAccountOpt.isEmpty()) {
            return Optional.empty();
        }

        // 2. LolAccount에서 User 엔티티 획득 및 Discord ID 추출
        User user = lolAccountOpt.get().getUser();
        if (user == null) {
            log.warn("Found LolAccount {}#{} but it is not linked to any User entity.", summonerName, tagLine);
            return Optional.empty();
        }
        Long discordUserId = user.getDiscordUserId();

        // 3. Discord ID로 통계 조회 로직 재사용
        return searchUserInternalStatsByDiscordId(discordUserId, serverId);
    }

    @Override
    public Optional<UserSearchDto> searchUserInternalStatsByDiscordId(Long discordUserId, Long serverId) {

        // 0. User 엔티티 조회 (Riot ID 정보 및 LolAccounts 접근을 위해 필요)
        Optional<User> userOpt = userRepository.findByDiscordUserId(discordUserId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        // 1. 해당 유저의 서버 종합 통계 데이터 조회 (UserServerStats)
        Optional<UserServerStats> overallStatsOpt = userServerStatsRepository
                .findByUser_DiscordUserIdAndGuildServer_DiscordServerId(discordUserId, serverId);

        if (overallStatsOpt.isEmpty()) {
            return Optional.empty(); // 서버에 기록된 내전 통계 없음
        }

        UserServerStats overallStats = overallStatsOpt.get();

        // 2. UserRankDto의 from() 메서드를 사용하여 종합 지표 계산 로직 재활용
        UserRankDto overallMetrics = UserRankDto.from(overallStats);

        // 3. 챔피언별 상세 통계 데이터 조회 (ChampionStats)
        List<ChampionStats> championRawStats = championStatsRepository
                .findAllByUser_DiscordUserIdAndGuildServer_DiscordServerId(discordUserId, serverId);

        // 4. ChampionSearchDto로 변환 및 리스트 생성
        List<ChampionSearchDto> championStatsList = championRawStats.stream()
                .filter(stats -> stats.getTotalGames() > 0)
                .map(ChampionSearchDto::from)
                .sorted((a, b) -> Integer.compare(b.getTotalGames(), a.getTotalGames()))
                .collect(Collectors.toList());

        // 5. User 엔티티에서 대표 Riot ID 정보 및 모든 계정 목록 추출
        Set<LolAccount> accounts = user.getLolAccounts();
        String displaySummonerName = "N/A";
        String displayTagLine = "N/A";

        // 연결된 모든 계정 목록 (출력용)
        List<String> linkedAccountNames = accounts.stream()
                .map(account -> account.getGameName() + "#" + account.getTagLine())
                .collect(Collectors.toList());

        if (!accounts.isEmpty()) {
            // 대표 계정 설정 (첫 번째 계정)
            LolAccount representativeAccount = accounts.iterator().next();
            displaySummonerName = representativeAccount.getGameName();
            displayTagLine = representativeAccount.getTagLine();
        }


        // 6. 최종 UserSearchDto 구성
        return Optional.of(UserSearchDto.builder()
                .discordUserId(discordUserId)
                .summonerName(displaySummonerName)
                .lolTagLine(displayTagLine)
                .linkedLolAccounts(linkedAccountNames) // ⭐ 모든 계정 목록 포함

                // 종합 통계 지표
                .kda(overallMetrics.getKda())
                .gpm(overallMetrics.getGpm())
                .dpm(overallMetrics.getDpm())
                .winRate(overallMetrics.getWinRate())
                .killParticipation(overallMetrics.getKillParticipation())
                .totalGames(overallMetrics.getTotalGames())

                // 챔피언별 통계 목록
                .championStatsList(championStatsList)

                // 기타 필드
                .soloRankTier("N/A")
                .flexRankTier("N/A")
                .metricRanks(Collections.emptyMap())
                .build());
    }
}