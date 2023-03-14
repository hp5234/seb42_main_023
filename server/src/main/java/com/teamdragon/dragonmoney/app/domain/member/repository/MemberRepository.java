package com.teamdragon.dragonmoney.app.domain.member.repository;

import com.teamdragon.dragonmoney.app.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, String> {
    Optional<Member> findByName(String name);
    Optional<Member> findByEmailAndOauthkind(String email, String oauthkind);
    Member findByTempName(String tempName);
    Optional<Member> findByNameDuplicateCheckAndEmailAndOauthkind(Boolean nameDuplicateCheck, String email, String oauthkind);
}