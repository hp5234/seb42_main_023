package com.teamdragon.dragonmoney.app.domain.comment.service;

import com.teamdragon.dragonmoney.app.domain.comment.entity.Comment;
import com.teamdragon.dragonmoney.app.domain.comment.repository.CommentRepository;
import com.teamdragon.dragonmoney.app.domain.common.service.FinderService;
import com.teamdragon.dragonmoney.app.domain.delete.entity.DeleteResult;
import com.teamdragon.dragonmoney.app.domain.member.entity.Member;
import com.teamdragon.dragonmoney.app.domain.posts.entity.Posts;
import com.teamdragon.dragonmoney.app.domain.reply.service.ReplyHandleServiceImpl;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbCountService;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbDto;
import com.teamdragon.dragonmoney.app.global.exception.AuthExceptionCode;
import com.teamdragon.dragonmoney.app.global.exception.AuthLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Transactional
@Service
public class CommentHandleServiceImpl implements CommentHandleService,ThumbCountService {

    private final CommentRepository commentRepository;
    private final CommentFindService commentFindService;
    private final FinderService finderService;
    private final ReplyHandleServiceImpl replyService;

    // 추가
    @Override
    public Comment createComment(Long postsId, Member loginMember, Comment newComment) {
        Posts parentPosts = finderService.findVerifyPostsById(postsId);
        parentPosts.plusCommentCount();
        Comment comment = Comment.builder()
                .content(newComment.getContent())
                .writer(loginMember)
                .posts(parentPosts)
                .build();
        return commentRepository.save(comment);
    }

    // 삭제
    @Override
    public Long removeComment(Member loginMember, Long commentId) {
        Comment findComment = checkOwner(loginMember, commentId);
        Posts parentPosts = findComment.getPosts();
        DeleteResult deleteResult
                = DeleteResult.builder().deleteReason(DeleteResult.Reason.SELF_DELETED).build();
        findComment.changeStateToDeleted(deleteResult);
        return commentId;
    }

    // 여러 댓글 삭제 : 부모 삭제로 인한 삭제
    @Override
    public void removeCommentsByParent(List<Comment> comments) {
        for (Comment comment : comments) {
            comment.changeStateToDeleted(new DeleteResult(DeleteResult.Reason.DELETED_BY_PARENT));
            replyService.removeReplyListByParent(comment.getReplies());
        }
        commentRepository.saveAll(comments);
    }

    // 신고로 인한 삭제
    @Override
    public void removeCommentByReport(Comment comment) {
        DeleteResult deleteResult
                = DeleteResult.builder().deleteReason(DeleteResult.Reason.DELETED_BY_REPORT).build();
        comment.changeStateToDeleted(deleteResult);
        // 답글 삭제
        replyService.removeReplyListByParent(comment.getReplies());
        commentRepository.save(comment);
    }

    // 회원 탈퇴로 인한 댓글 삭제
    @Override
    public void removeCommentByDeletedMember(String memberName) {
        DeleteResult deleteResult = DeleteResult.builder()
                .deleteReason(DeleteResult.Reason.DELETED_BY_MEMBER_REMOVE)
                .build();

        List<Comment> comments = commentRepository.findCommentByDeletedMember(memberName);
        for(Comment addDeletedResultByComment : comments) {
            addDeletedResultByComment.changeStateToDeleted(deleteResult);
        }

        commentRepository.saveAll(comments);
    }

    // 수정
    @Override
    public Comment modifyComment(Member loginMember, Long commentId, Comment updateComment) {
        Comment originalComment = checkOwner(loginMember, commentId);
        originalComment.isModifiedNow();
        originalComment.updateContent(updateComment.getContent());
        return commentRepository.save(originalComment);
    }

    @Override
    public ThumbDto modifyThumbupState(Long commentId, boolean needInquiry, ThumbDto.ACTION action) {
        Comment findComment = commentFindService.findVerifyCommentById(commentId);
        if (action == ThumbDto.ACTION.PLUS) {
            findComment.plusThumbupCount();
        } else if ( action == ThumbDto.ACTION.MINUS) {
            findComment.minusThumbupCount();
        }
        Comment updateComment = commentRepository.save(findComment);
        if (needInquiry) {
            return updateComment.getThumbCount();
        }
        return null;
    }

    @Override
    public ThumbDto modifyThumbdownState(Long commentId, boolean needInquiry, ThumbDto.ACTION action) {
        Comment findComment = commentFindService.findVerifyCommentById(commentId);
        if (action == ThumbDto.ACTION.PLUS) {
            findComment.plusThumbdownCount();
        } else if ( action == ThumbDto.ACTION.MINUS) {
            findComment.minusThumbdownCount();
        }
        Comment updateComment = commentRepository.save(findComment);
        if (needInquiry) {
            return updateComment.getThumbCount();
        }
        return null;
    }

    // 작성자 확인
    private Comment checkOwner(Member loginMember, Long commenmtId) {
        Comment findComment = commentFindService.findVerifyCommentById(commenmtId);
        if (!findComment.getWriter().getId().equals(loginMember.getId())) {
            throw new AuthLogicException(AuthExceptionCode.AUTHORIZED_FAIL);
        }
        return findComment;
    }
}
