package com.teamdragon.dragonmoney.app.global.auth.service;

import com.teamdragon.dragonmoney.app.domain.common.service.FinderService;
import com.teamdragon.dragonmoney.app.domain.member.entity.Member;
import com.teamdragon.dragonmoney.app.domain.member.repository.MemberRepository;
import com.teamdragon.dragonmoney.app.domain.member.service.MemberFindService;
import com.teamdragon.dragonmoney.app.global.auth.dto.LoginResponseDto;
import com.teamdragon.dragonmoney.app.global.auth.jwt.JwtTokenizer;
import com.teamdragon.dragonmoney.app.global.auth.refresh.entity.RefreshToken;
import com.teamdragon.dragonmoney.app.global.auth.refresh.repository.RefreshTokenRepository;
import com.teamdragon.dragonmoney.app.global.exception.AuthExceptionCode;
import com.teamdragon.dragonmoney.app.global.exception.AuthLogicException;
import com.teamdragon.dragonmoney.app.global.exception.BusinessExceptionCode;
import com.teamdragon.dragonmoney.app.global.exception.BusinessLogicException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RequiredArgsConstructor
@Transactional
@Service
public class OAuth2Service {
    private final JwtTokenizer jwtTokenizer;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final MemberFindService memberFindService;
    private final FinderService finderService;

    // Temp Access Token 발급
    public String delegateTempAccessToken(String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", name);

        String subject = name;
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getTempAccessTokenExpirationMinutes());

        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        return accessToken;
    }

    // Access Token 발급
    public String delegateAccessToken(String tempAccessToken) {
        Member member = findMemberByTempAccessToken(tempAccessToken);
        String name = member.getName();
        List<String> roles = member.getRoles();

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", name);
        claims.put("roles", roles);

        String subject = name;
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMinutes());

        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        return accessToken;
    }

    // AccessToken 재발급
    public String delegateAccessTokenAgain(String memberName) {
        Member member = finderService.findVerifiedMemberByName(memberName);
        List<String> roles = member.getRoles();

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", memberName);
        claims.put("roles", roles);

        String subject = memberName;
        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getAccessTokenExpirationMinutes());

        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String accessToken = jwtTokenizer.generateAccessToken(claims, subject, expiration, base64EncodedSecretKey);

        return accessToken;
    }

    // RefreshToken 발급
    public String delegateRefreshToken(String tempAccessToken) {
        Member member = findMemberByTempAccessToken(tempAccessToken);
        String name = member.getName();

        Date expiration = jwtTokenizer.getTokenExpiration(jwtTokenizer.getRefreshTokenExpirationMinutes());
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());

        String refreshToken = jwtTokenizer.generateRefreshToken(name, expiration, base64EncodedSecretKey);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .member(member)
                .refreshTokenValue(refreshToken)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        member.saveRefreshToken(refreshTokenEntity);

        return refreshToken;
    }

    //tempAccessToken 저장
    public Member updateTempAccessToken(String name, String tempAccessToken) {
        Member member = memberFindService.findVerifiedMemberName(name);
        member.saveTempAccessToken(tempAccessToken);

        return memberRepository.save(member);
    }

    // Refresh Token 검증
    public void verifyJws(HttpServletRequest request) {
        try {
            Map<String, Object> claims = getMemberNameFromRefreshToken(request);
        } catch (SignatureException se) {
            throw new AuthLogicException(AuthExceptionCode.REFRESH_TOKEN_INVALID);
        } catch (ExpiredJwtException ee) {
            throw new AuthLogicException(AuthExceptionCode.REFRESH_TOKEN_EXPIRED);
        } catch (Exception e) {
            throw new AuthLogicException(AuthExceptionCode.USER_UNAUTHORIZED);
        }
    }

    // Resresh Token 파싱
    public Map<String, Object> getMemberNameFromRefreshToken(HttpServletRequest request) {
        String jws = request.getHeader("Refresh");
        String base64EncodedSecretKey = jwtTokenizer.encodeBase64SecretKey(jwtTokenizer.getSecretKey());
        Map<String, Object> claims = jwtTokenizer.getClaims(jws, base64EncodedSecretKey).getBody();

        return claims;
    }

    // 로그인 정보 찾기
    public LoginResponseDto findLoginMember(String tempAccessToken) {
        Member member = findMemberByTempAccessToken(tempAccessToken);
        String name = member.getName();
        String picture = member.getProfileImage();
        List<String> roles = member.getRoles();
        String role = roles.get(0);

        LoginResponseDto loginResponseDto = new LoginResponseDto();
        loginResponseDto.setName(name);
        loginResponseDto.setPicture(picture);
        loginResponseDto.setRole(role);
        return loginResponseDto;
    }

    // 탈퇴된 회원 복구
    public Member changeMemberStateToActive(String tempAccessToken) {
        Member member = findMemberByTempAccessToken(tempAccessToken);
        member.changedMemberState(Member.MemberState.ACTIVE);

        return memberRepository.save(member);
    }

    // 해당 임시 토큰을 가진 회원이 있는지 조회
    public Member findMemberByTempAccessToken(String tempAccessToken) {
        Optional<Member> optionalMember = memberRepository.findByTempAccessToken(tempAccessToken);

        return optionalMember
                .orElseThrow( () -> new BusinessLogicException(BusinessExceptionCode.USER_NOT_FOUND));
    }

    // 회원 이름으로 refreshToken 조회
    public String findRefreshTokenByMemberName(String memberName) {
        Member member = memberFindService.findMember(memberName);
        String refreshToken = member.getRefreshToken().getRefreshTokenValue();

        return refreshToken;
    }
}