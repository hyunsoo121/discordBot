package com.discordBot.demo.service.impl;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.GuildServer; // GuildServer 엔티티 임포트 필요
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.UserRepository;
import com.discordBot.demo.service.RiotApiService;
import com.discordBot.demo.service.ServerManagementService;
import com.discordBot.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final LolAccountRepository lolAccountRepository;
    private final RiotApiService riotApiService;
    private final ServerManagementService serverManagementService; // ⭐ ServerManagementService 주입

    /**
     * 관리자가 대상 유저의 롤 계정을 대신 등록합니다. (서버별 등록)
     * @param targetDiscordUserId 롤 계정을 연결할 대상 디스코드 유저 ID
     * @param gameName 롤 게임 이름
     * @param tagLine 롤 태그라인
     * @param discordServerId 계정이 등록될 디스코드 서버 ID
     * @return 등록 완료 메시지
     */
    @Override
    @Transactional
    public String registerLolNickname(Long targetDiscordUserId, String gameName, String tagLine, Long discordServerId) {

        // TagLine이 없으면 빈 문자열로 표준화
        if (!StringUtils.hasText(tagLine)) {
            tagLine = "";
        }

        // 1. Riot API를 통해 계정 유효성 검증 및 Puuid 획득 (생략 없음)
        Optional<RiotAccountDto> riotAccountOpt = riotApiService.verifyNickname(gameName, tagLine);

        if (riotAccountOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "❌ 오류: Riot Games에 **" + gameName + "#" + tagLine + "**에 해당하는 계정이 존재하지 않습니다. 닉네임을 다시 확인해 주세요."
            );
        }

        RiotAccountDto riotAccount = riotAccountOpt.get();
        String puuid = riotAccount.getPuuid();

        String verifiedGameName = riotAccount.getGameName();
        String verifiedTagLine = riotAccount.getTagLine();

        // 2. 대상 디스코드 사용자 찾기 또는 생성
        User targetUser = userRepository.findByDiscordUserId(targetDiscordUserId)
                .orElseGet(() -> {
                    log.info("새로운 대상 디스코드 유저 등록: ID={}", targetDiscordUserId);
                    User newUser = new User();
                    newUser.setDiscordUserId(targetDiscordUserId);
                    // (TODO: 유저 이름 설정 로직 필요)
                    return userRepository.save(newUser);
                });

        // 2.5. ⭐ GuildServer 엔티티 확보 (LolAccount에 연결하기 위해 필수)
        // 해당 서버가 DB에 없으면 생성합니다.
        GuildServer guildServer = serverManagementService.findOrCreateGuildServer(discordServerId);


        // 3. ⭐ 롤 계정 중복 확인 (GameName + TagLine + Server ID 조합으로 확인)
        Optional<LolAccount> existingAccountOpt = lolAccountRepository.findByGameNameAndTagLineAndGuildServer_DiscordServerId(
                verifiedGameName,
                verifiedTagLine,
                discordServerId // ⭐ DiscordServerId를 사용하여 서버별 중복 확인
        );

        if (existingAccountOpt.isPresent()) {
            LolAccount existingAccount = existingAccountOpt.get();

            // 3-1. 소유권 충돌 검사: 이미 다른 유저가 소유한 경우
            if (existingAccount.getUser() != null && !existingAccount.getUser().equals(targetUser)) {
                // 이 서버 내에서 이 계정은 이미 다른 유저에게 등록되어 있습니다.
                throw new IllegalArgumentException(
                        "❌ 오류: 이 서버 내 롤 계정 **" + existingAccount.getFullAccountName() +
                                "**은 이미 다른 사용자에게 등록되어 있어 소유권을 변경할 수 없습니다."
                );
            }

            // 3-2. 계정은 있으나 연결 유저가 없는 경우 또는 이미 연결된 경우
            existingAccount.setUser(targetUser);
            existingAccount.setPuuid(puuid);
            existingAccount.setGameName(verifiedGameName);
            existingAccount.setTagLine(verifiedTagLine);
            existingAccount.setGuildServer(guildServer); // ⭐ GuildServer 설정 추가
            lolAccountRepository.save(existingAccount);

            return "✅ 관리자 등록 완료: 롤 계정 **" + existingAccount.getFullAccountName() +
                    "**가 대상 유저에게 연결되었습니다! (서버 ID: " + discordServerId + ")";
        }

        // 4. 신규 롤 계정 등록
        LolAccount newAccount = new LolAccount();
        newAccount.setGameName(verifiedGameName);
        newAccount.setTagLine(verifiedTagLine);
        newAccount.setPuuid(puuid);
        newAccount.setUser(targetUser);
        newAccount.setGuildServer(guildServer); // ⭐ GuildServer 설정 추가

        lolAccountRepository.save(newAccount);

        return "🎉 관리자 등록 완료: 롤 계정 **" + newAccount.getFullAccountName() +
                "**가 대상 유저에게 성공적으로 등록되었습니다!";
    }

    // --- 유틸리티 메서드: 기존 LolAccount에 Discord User 연결 ---
    @Override
    @Transactional
    public String linkExistingAccount(Long targetDiscordUserId, Long lolAccountId) {

        // 1. 대상 LolAccount 조회
        LolAccount lolAccount = lolAccountRepository.findById(lolAccountId)
                .orElseThrow(() -> new IllegalArgumentException("❌ 오류: 선택한 롤 계정 ID(" + lolAccountId + ")를 찾을 수 없습니다."));

        // 2. 대상 User 조회 또는 생성
        User targetUser = userRepository.findByDiscordUserId(targetDiscordUserId)
                .orElseGet(() -> {
                    log.info("새로운 대상 디스코드 유저 등록: ID={}", targetDiscordUserId);
                    User newUser = new User();
                    newUser.setDiscordUserId(targetDiscordUserId);
                    return userRepository.save(newUser);
                });

        // 3. 소유권 확인 및 업데이트
        User existingOwner = lolAccount.getUser();

        if (existingOwner != null && !existingOwner.equals(targetUser)) {
            // 이미 다른 유저가 소유한 경우
            throw new IllegalArgumentException("❌ 오류: 롤 계정 **" + lolAccount.getFullAccountName() +
                    "**는 이미 다른 사용자에게 연결되어 있어 소유권을 변경할 수 없습니다.");
        }

        // 연결 업데이트 (Puuid는 이미 해당 계정에 있을 것으로 가정)
        lolAccount.setUser(targetUser);
        lolAccountRepository.save(lolAccount);

        return "✅ 롤 계정 **" + lolAccount.getFullAccountName() +
                "**가 대상 유저(" + targetDiscordUserId + ")에게 성공적으로 연결되었습니다!";
    }
}
