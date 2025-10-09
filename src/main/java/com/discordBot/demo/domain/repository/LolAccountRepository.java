package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LolAccount;
import com.discordBot.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LolAccountRepository extends JpaRepository<LolAccount, Long> {

    /**
     * 특정 롤 계정(gameName과 tagLine 조합)이 이미 등록되었는지 확인합니다.
     * @param gameName 롤 게임 이름
     * @param tagLine 롤 태그
     * @return LolAccount 엔티티 (존재하지 않으면 Empty)
     */
    Optional<LolAccount> findByGameNameAndTagLine(String gameName, String tagLine);

    /**
     * 특정 유저가 이미 해당 롤 계정을 등록했는지 확인합니다.
     * @param user 디스코드 유저 엔티티
     * @param gameName 롤 게임 이름
     * @param tagLine 롤 태그
     * @return 존재 여부
     */
    boolean existsByUserAndGameNameAndTagLine(User user, String gameName, String tagLine);
}