package com.discordBot.demo.domain.repository;

import com.discordBot.demo.domain.entity.Line;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LineRepository extends JpaRepository<Line, Long> {

    /** 라인 이름(TOP, JUNGLE)으로 엔티티를 조회합니다. */
    Optional<Line> findByName(String name);

    List<Line> findAll();
}