package com.teamdragon.dragonmoney.app.domain.popular.service;

import com.teamdragon.dragonmoney.app.domain.popular.repository.BestAwardsRepository;
import com.teamdragon.dragonmoney.app.domain.popular.entity.BestAwards;
import com.teamdragon.dragonmoney.app.domain.posts.dto.PostsDto;
import com.teamdragon.dragonmoney.app.domain.posts.entity.Posts;
import com.teamdragon.dragonmoney.app.domain.posts.repository.PostsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@Service
public class PopularService {

    private final PostsRepository postsRepository;
    private final BestAwardsRepository bestAwardsRepository;

    @Getter
    private LocalDateTime countStartedAt;
    @Getter
    private LocalDateTime countEndedAt;

    private static final int WEEKLY_POPULAR_MAX_SIZE = 3;
    private static final int RECOMMEND_POSTS_MAX_SIZE = 10;
    private static final int PAGE_ELEMENT_SIZE = 10;

    public PopularService(PostsRepository postsRepository, BestAwardsRepository bestAwardsRepository) {
        this.postsRepository = postsRepository;
        this.bestAwardsRepository = bestAwardsRepository;
        setStartTime();
        setEndTime(countStartedAt);
    }

    // 주간 인기 게시물 조회
    public PostsDto.WeeklyPopularRes findWeeklyPopularListDto() {
        List<Posts> weeklyPopularList = findWeeklyPopularList();
        return PostsDto.WeeklyPopularRes.builder()
                .posts(weeklyPopularList)
                .start(countStartedAt)
                .end(countEndedAt)
                .build();
    }

    public void saveBestAwardsList(ArrayList<BestAwards> bestAwards) {
        bestAwardsRepository.saveAll(bestAwards);
    }

    public List<Posts> findWeeklyPopularList() {
        return postsRepository.findWeeklyPopularList(WEEKLY_POPULAR_MAX_SIZE, countStartedAt, countEndedAt);
    }

    // 명예의 전당 목록 조회
    public PostsDto.BestAwardsRes findBestAwardsList(int page, Posts.OrderBy orderBy) {
        Pageable pageable = PageRequest.of(page - 1 , PAGE_ELEMENT_SIZE, Sort.by(orderBy.getTargetProperty()).descending());
        Page<Posts> bestAwardsList = postsRepository.findBestAwardsListByPage(pageable);
        return new PostsDto.BestAwardsRes(bestAwardsList, orderBy.getOrderBy());
    }

    // 추천 게시물 목록 조회
    public PostsDto.RecommendPostsListRes findRecommendPosts() {
        List<Posts> recommendPostsList = postsRepository.findRecommendPostsList(RECOMMEND_POSTS_MAX_SIZE);
        return new PostsDto.RecommendPostsListRes(recommendPostsList);
    }

    // 주간인기글 측정 시작시간 설정
    public void setStartTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        LocalDateTime tempTime = currentTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        countStartedAt =
        LocalDateTime.of(tempTime.getYear(), tempTime.getMonth(), tempTime.getDayOfMonth(), 0, 0, 0);
    }

    // 주간인기글 종료 시작시간 설정
    public void setEndTime(LocalDateTime startTime) {
        countEndedAt = startTime.plusDays(7);
    }
}
