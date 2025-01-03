package com.teamdragon.dragonmoney.app.domain.posts.service;

import com.teamdragon.dragonmoney.app.domain.category.entity.Category;
import com.teamdragon.dragonmoney.app.domain.category.service.CategoryFindService;
import com.teamdragon.dragonmoney.app.domain.comment.service.CommentHandleService;
import com.teamdragon.dragonmoney.app.domain.image.entity.Image;
import com.teamdragon.dragonmoney.app.domain.image.service.ImageFindService;
import com.teamdragon.dragonmoney.app.domain.image.service.ImageHandleService;
import com.teamdragon.dragonmoney.app.domain.member.entity.Member;
import com.teamdragon.dragonmoney.app.domain.posts.entity.Posts;
import com.teamdragon.dragonmoney.app.domain.posts.entity.PostsTag;
import com.teamdragon.dragonmoney.app.domain.posts.repository.PostsRepository;
import com.teamdragon.dragonmoney.app.domain.posts.repository.PostsTagRepository;
import com.teamdragon.dragonmoney.app.domain.tag.entity.Tag;
import com.teamdragon.dragonmoney.app.domain.tag.service.TagHandleService;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbCountable;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbDto;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbCountService;
import com.teamdragon.dragonmoney.app.domain.delete.entity.DeleteResult;
import com.teamdragon.dragonmoney.app.domain.thumb.entity.Thumb;
import com.teamdragon.dragonmoney.app.domain.thumb.service.ThumbCountAction;
import com.teamdragon.dragonmoney.app.global.exception.AuthExceptionCode;
import com.teamdragon.dragonmoney.app.global.exception.AuthLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
public class PostsHandleServiceImpl implements PostsHandleService, ThumbCountService {

    private final PostsFindService postsFindService;
    private final PostsRepository postsRepository;
    private final PostsTagRepository postsTagRepository;
    private final CategoryFindService categoryFindService;
    private final CommentHandleService commentHandleService;
    private final TagHandleService tagHandleService;
    private final ImageFindService imageFindService;
    private final ImageHandleService imageHandleService;

    private static final Long CURRENT_CATEGORY_ID = 1L;

    // 게시글 생성
    @Override
    public Posts savePosts(Member loginMember, Posts newPosts, List<Image> removedImages){
        // 카테고리 조회
        Category findCategory = categoryFindService.findCategoryById(CURRENT_CATEGORY_ID);
        // 이미지 삭제
        imageHandleService.removeImageList(loginMember, removedImages);

        // 업로드 이미지 조회
        List<Image> findImages = new ArrayList<>();
        List<Image> newPostsImages = newPosts.getImages();
        if (newPostsImages != null && !newPostsImages.isEmpty()) {
            findImages = imageFindService.findImageList(newPostsImages);
        }
        // 태그 조회 및 저장
        List<PostsTag> newPostsPostsTags = newPosts.getPostsTags();
        List<PostsTag> postsTags = new ArrayList<>();
        if (newPostsPostsTags != null && !newPostsPostsTags.isEmpty()) {
            List<String> tagNames = newPostsPostsTags.stream()
                    .map(postsTag -> postsTag.getTag().getName())
                    .collect(Collectors.toList());
            postsTags = getPostsTagList(tagNames);
        }

        Posts posts = Posts.builder()
                .writer(loginMember)
                .category(findCategory)
                .images(findImages)
                .postsTags(postsTags)
                .title(newPosts.getTitle())
                .content(newPosts.getContent())
                .build();
        return postsRepository.save(posts);
    }

    // 게시글 북마크 수 빼기
    @Override
    public void minusBookmarkCount(Posts posts) {
        posts.minusBookmarkCount();
        postsRepository.save(posts);
    }

    // 게시글 삭제
    @Override
    public Long removePosts(Member loginMember, Long postsId){
        Posts findPosts = checkOwner(loginMember, postsId);
        DeleteResult deleteResult = DeleteResult.builder()
                .deleteReason(DeleteResult.Reason.SELF_DELETED)
                .build();
        findPosts.changeStateToDeleted(deleteResult);

        // 태그 삭제
        postsTagRepository.deleteByPosts_Id(findPosts.getId());
        tagHandleService.removeOrphanTag();
        // 이미지 삭제
        imageHandleService.removeImageList(loginMember, findPosts.getImages());
        // 댓글 삭제 처리
        commentHandleService.removeCommentsByParent(findPosts.getComments());

        postsRepository.save(findPosts);
        return postsId;
    }

    // 신고에 의한 게시글 삭제
    @Override
    public void removeRepostedPosts(Member member, Posts posts) {
        DeleteResult deleteResult = DeleteResult.builder()
                .deleteReason(DeleteResult.Reason.DELETED_BY_REPORT)
                .build();
        posts.changeStateToDeleted(deleteResult);

        // 태그 삭제
        postsTagRepository.deleteByPosts_Id(posts.getId());
        tagHandleService.removeOrphanTag();
        // 이미지 삭제
        imageHandleService.removeImageList(member, posts.getImages());
        // 댓글 삭제 처리
        commentHandleService.removeCommentsByParent(posts.getComments());

        postsRepository.save(posts);
    }

    // 회원 탈퇴로 인한 삭제
    @Override
    public void removePostsByDeletedMember(String memberName) {
        DeleteResult deleteResult = DeleteResult.builder()
                .deleteReason(DeleteResult.Reason.DELETED_BY_MEMBER_REMOVE)
                .build();

        List<Posts> posts = postsRepository.findPostsByDeletedMember(memberName);
        for(Posts addDeletedResultByPosts : posts) {
            addDeletedResultByPosts.changeStateToDeleted(deleteResult);
        }

        postsRepository.saveAll(posts);
    }

    // 게시글 수정
    @Override
    public Posts updatePosts(Member loginMember, Long postsId, Posts updatePosts, List<Image> removedImages){
        Posts originalPosts = checkOwner(loginMember, postsId);
        originalPosts.isModifiedNow();

        // 이미지 처리
        imageHandleService.removeImageList(loginMember, removedImages);
        // 태그 처리
        List<PostsTag> postsTags = updateTagList(updatePosts, originalPosts);
        if (postsTags != null) {
            originalPosts.addPostsTags(postsTags);
            postsTagRepository.saveAll(postsTags);
        }
        // 추가된 이미지 : 이미지 추가
        List<Image> newImages = imageFindService.findImageList(updatePosts.getImages());
        if (newImages != null && !newImages.isEmpty()) {
            for (Image newImage : newImages) {
                originalPosts.addImage(newImage);
            }
        }

        originalPosts.updateTitle(updatePosts.getTitle());
        originalPosts.updateContent(updatePosts.getContent());
        return postsRepository.save(originalPosts);
    }

    // PostsTag 획득
    @Override
    public List<PostsTag> getPostsTagList(List<String> tagNames) {
        List<Tag> tags = saveTagList(tagNames);
        // PostsTag 설정
        List<PostsTag> postsTags = new ArrayList<>();
        for (Tag tag : tags) {
            postsTags.add(new PostsTag(null, tag));
        }
        return postsTags;
    }

    // update 로 인한 태그 삭제 및 추가 처리
    @Override
    public List<PostsTag> updateTagList(Posts updatePosts, Posts originalPosts) {
        List<String> updateTagNames = updatePosts.getTagNames();
        List<String> originalTagNames = originalPosts.getTagNames();

        if (originalTagNames.isEmpty() && updateTagNames.isEmpty()) {
            return null;
        } else if (!originalTagNames.isEmpty() && updateTagNames.isEmpty()) {
            postsTagRepository.deleteAllByPostsIdAndTagName(originalPosts.getId(), originalTagNames);
            originalPosts.removeAllPostsTag();
            return null;
        } else if (originalTagNames.isEmpty() && !updateTagNames.isEmpty()) {
            return getPostsTagList(updateTagNames);
        } else {
            List<String> newTagNames = new ArrayList<>();
            List<String> removedTagNames = new ArrayList<>();
            for (String updateTagName : updateTagNames) {
                if (!originalTagNames.contains(updateTagName)) {
                    newTagNames.add(updateTagName);
                }
            }
            for (String originalTagName : originalTagNames) {
                if (!updateTagNames.contains(originalTagName)) {
                    removedTagNames.add(originalTagName);
                }
            }
            postsTagRepository.deleteAllByPostsIdAndTagName(originalPosts.getId(), removedTagNames);
            return getPostsTagList(newTagNames);
        }
    }

    // 작성자 확인
    @Override
    public Posts checkOwner(Member loginMember, Long postsId) {
        Posts findPosts = postsFindService.findVerifyPostsById(postsId);
        if (!findPosts.getWriter().getId().equals(loginMember.getId())) {
            throw new AuthLogicException(AuthExceptionCode.AUTHORIZED_FAIL);
        }
        return findPosts;
    }

    // Tags 조회 및 저장
    @Override
    public List<Tag> saveTagList(List<String> tagNames) {
        return tagHandleService.saveListTag(tagNames);
    }

    // 좋아요 상태 수정
    @Override
    public ThumbDto modifyThumbState(Long postsId, Thumb.Type thumbType, ThumbCountAction action, boolean isChange) {
        Posts findPosts = postsFindService.findVerifyPostsById(postsId);
        if (thumbType == Thumb.Type.UP) {
            return modifyThumbupState(findPosts, action, isChange);
        } else {
            return modifyThumbdownState(findPosts, action, isChange);
        }
    }

    // 좋아요 상태 수정 : 좋아요
    @Override
    public ThumbDto modifyThumbupState(ThumbCountable thumbablePosts, ThumbCountAction action, boolean isChange) {
        Posts posts = (Posts) thumbablePosts;
        if (action == ThumbCountAction.PLUS) {
            posts.plusThumbupCount();
            if (isChange) {
                posts.minusThumbdownCount();
            }
        } else if ( action == ThumbCountAction.MINUS) {
            posts.minusThumbupCount();
            if (isChange) {
                posts.plusThumbdownCount();
            }
        }
        Posts updatePosts = postsRepository.save(posts);
        return updatePosts.getThumbCount();
    }

    // 좋아요 상태 수정 : 싫어요
    @Override
    public ThumbDto modifyThumbdownState(ThumbCountable thumbablePosts, ThumbCountAction action, boolean isChange) {
        Posts posts = (Posts) thumbablePosts;
        if (action == ThumbCountAction.PLUS) {
            posts.plusThumbdownCount();
            if (isChange) {
                posts.minusThumbupCount();
            }
        } else if ( action == ThumbCountAction.MINUS) {
            posts.minusThumbdownCount();
            if (isChange) {
                posts.plusThumbupCount();
            }
        }
        Posts updatePosts = postsRepository.save(posts);
        return updatePosts.getThumbCount();
    }
}