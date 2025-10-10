package com.discordBot.demo.service;

import com.discordBot.demo.domain.dto.RiotAccountDto;
import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import com.discordBot.demo.domain.repository.LolAccountRepository;
import com.discordBot.demo.domain.repository.UserRepository;
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
    private final RiotApiService riotApiService; // ⭐ RiotApiService 주입

    /**
     * 관리자가 대상 유저의 롤 계정을 대신 등록합니다.
     * 이 서비스는 디스코드 봇 계층에서 관리자 권한이 확인된 후 호출됩니다.
     */
    @Override
    @Transactional
    public String registerLolNickname(Long targetDiscordUserId, String gameName, String tagLine) {

        // TagLine이 없으면 빈 문자열로 표준화 (API 호출 전에는 원본을 사용)
        if (!StringUtils.hasText(tagLine)) {
            tagLine = "";
        }

        // 1. Riot API를 통해 계정 유효성 검증 및 Puuid 획득
        Optional<RiotAccountDto> riotAccountOpt = riotApiService.verifyNickname(gameName, tagLine);

        if (riotAccountOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "❌ 오류: Riot Games에 **" + gameName + "#" + tagLine + "**에 해당하는 계정이 존재하지 않습니다. 닉네임을 다시 확인해 주세요."
            );
        }

        RiotAccountDto riotAccount = riotAccountOpt.get();
        String puuid = riotAccount.getPuuid();

        // API 응답으로 받은 GameName과 TagLine을 사용 (최신 정보 및 대소문자 일치)
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

        // 3. 롤 계정 중복 확인 (검증된 GameName과 TagLine 사용)
        Optional<LolAccount> existingAccountOpt = lolAccountRepository.findByGameNameAndTagLine(verifiedGameName, verifiedTagLine);

        if (existingAccountOpt.isPresent()) {
            LolAccount existingAccount = existingAccountOpt.get();

            // 3-1. 소유권 충돌 검사: 이미 다른 유저가 소유한 경우
            if (existingAccount.getUser() != null && !existingAccount.getUser().equals(targetUser)) {
                throw new IllegalArgumentException(
                        "❌ 오류: 롤 계정 **" + existingAccount.getFullAccountName() +
                                "**은 이미 다른 사용자에게 등록되어 있어 소유권을 변경할 수 없습니다."
                );
            }

            // 3-2. 계정은 있으나 연결 유저가 없는 경우 또는 이미 연결된 경우
            // Puuid 및 최신 GameName/TagLine 업데이트 후 대상 유저에게 연결
            existingAccount.setUser(targetUser);
            existingAccount.setPuuid(puuid);
            existingAccount.setGameName(verifiedGameName);
            existingAccount.setTagLine(verifiedTagLine);
            lolAccountRepository.save(existingAccount);

            return "✅ 관리자 등록 완료: 롤 계정 **" + existingAccount.getFullAccountName() +
                    "**가 대상 유저에게 연결되었습니다!";
        }

        // 4. 신규 롤 계정 등록
        LolAccount newAccount = new LolAccount();
        newAccount.setGameName(verifiedGameName);
        newAccount.setTagLine(verifiedTagLine);
        newAccount.setPuuid(puuid);
        newAccount.setUser(targetUser);

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