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
import java.util.Comparator; // ⭐ Comparator Import
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

        Optional<LolAccount> lolAccountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                summonerName,
                tagLine,
                serverId
        );

        if (lolAccountOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = lolAccountOpt.get().getUser();
        if (user == null) {
            log.warn("Found LolAccount {}#{} but it is not linked to any User entity.", summonerName, tagLine);
            return Optional.empty();
        }
        Long discordUserId = user.getDiscordUserId();

        return searchUserInternalStatsByDiscordId(discordUserId, serverId);
    }

    @Override
    public Optional<UserSearchDto> searchUserInternalStatsByDiscordId(Long discordUserId, Long serverId) {

        Optional<User> userOpt = userRepository.findByDiscordUserId(discordUserId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        Optional<UserServerStats> overallStatsOpt = userServerStatsRepository
                .findByUser_DiscordUserIdAndGuildServer_DiscordServerId(discordUserId, serverId);

        if (overallStatsOpt.isEmpty()) {
            return Optional.empty();
        }

        UserServerStats overallStats = overallStatsOpt.get();
        UserRankDto overallMetrics = UserRankDto.from(overallStats);

        List<ChampionStats> championRawStats = championStatsRepository
                .findAllByUser_DiscordUserIdAndGuildServer_DiscordServerId(discordUserId, serverId);

        Comparator<ChampionSearchDto> championComparator = Comparator
                // 1순위: 판수 (Total Games) - 내림차순
                .comparing(ChampionSearchDto::getTotalGames, Comparator.reverseOrder())
                // 2순위: 승률 (Win Rate) - 내림차순
                .thenComparing(ChampionSearchDto::getWinRate, Comparator.reverseOrder())
                // 3순위: KDA - 내림차순
                .thenComparing(ChampionSearchDto::getKda, Comparator.reverseOrder());

        List<ChampionSearchDto> championStatsList = championRawStats.stream()
                .filter(stats -> stats.getTotalGames() > 0)
                .map(ChampionSearchDto::from)
                .sorted(championComparator)
                .collect(Collectors.toList());

        Set<LolAccount> accounts = user.getLolAccounts();
        String displaySummonerName = "N/A";
        String displayTagLine = "N/A";

        List<String> linkedAccountNames = accounts.stream()
                .map(account -> account.getGameName() + "#" + account.getTagLine())
                .collect(Collectors.toList());

        if (!accounts.isEmpty()) {
            LolAccount representativeAccount = accounts.iterator().next();
            displaySummonerName = representativeAccount.getGameName();
            displayTagLine = representativeAccount.getTagLine();
        }

        return Optional.of(UserSearchDto.builder()
                .discordUserId(discordUserId)
                .summonerName(displaySummonerName)
                .lolTagLine(displayTagLine)
                .linkedLolAccounts(linkedAccountNames)

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