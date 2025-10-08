package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.LolNickname;
import com.discordBot.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LolNicknameRepository extends JpaRepository<LolNickname, Long> {
    /**
     * 특정 닉네임이 DB에 이미 존재하는지 확인합니다. (닉네임 중복 방지)
     * UserService의 중복 확인 로직에 사용됩니다.
     */
    boolean existsByNickname(String nickname);

    /**
     * 특정 유저(User 엔티티)가 등록한 닉네임의 개수를 반환합니다.
     * 첫 번째 닉네임을 대표 닉네임으로 설정하는 로직에 사용됩니다.
     */
    long countByUser(User user);
}
