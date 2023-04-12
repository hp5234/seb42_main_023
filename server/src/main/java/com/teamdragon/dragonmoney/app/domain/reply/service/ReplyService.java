package com.teamdragon.dragonmoney.app.domain.reply.service;

import com.teamdragon.dragonmoney.app.domain.comment.entity.Comment;
import com.teamdragon.dragonmoney.app.domain.common.service.FinderService;
import com.teamdragon.dragonmoney.app.domain.member.entity.Member;
import com.teamdragon.dragonmoney.app.domain.reply.dto.ReplyDto;
import com.teamdragon.dragonmoney.app.domain.reply.entity.Reply;
import com.teamdragon.dragonmoney.app.domain.reply.repository.ReplyRepository;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbDto;
import com.teamdragon.dragonmoney.app.domain.thumb.ThumbCountService;
import com.teamdragon.dragonmoney.app.domain.delete.entity.DeleteResult;
import com.teamdragon.dragonmoney.app.global.exception.AuthExceptionCode;
import com.teamdragon.dragonmoney.app.global.exception.AuthLogicException;
import com.teamdragon.dragonmoney.app.global.exception.BusinessExceptionCode;
import com.teamdragon.dragonmoney.app.global.exception.BusinessLogicException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Transactional
@Service
public class ReplyService implements ThumbCountService {

    private final ReplyRepository replyRepository;
    private final FinderService finderService;

    private static final int PAGE_ELEMENT_SIZE = 10;

    // 단일 조회 : Active 상태
    public Reply findOneStateActive(Long postsId) {
        Reply findReply = findVerifyReplyById(postsId);
        if (findReply.getState() != Reply.State.ACTIVE) {
            throw new BusinessLogicException(BusinessExceptionCode.THUMB_UNUSABLE);
        }
        return findReply;
    }

    // 추가
    public Reply createReply(Long commentId, Member loginMember, Reply newReply) {
        Comment findComment = finderService.findVerifyCommentById(commentId);
        findComment.plusReplyCount();
        Reply reply = Reply.builder()
                .content(newReply.getContent())
                .writer(loginMember)
                .comment(findComment)
                .build();
        return replyRepository.save(reply);
    }

    // 삭제
    public Long removeReply(Member loginMember, Long replyId) {
        Reply findReply = checkOwner(loginMember, replyId);
        DeleteResult deleteResult
                = DeleteResult.builder().deleteReason(DeleteResult.Reason.SELF_DELETED).build();
        findReply.changeStateToDeleted(deleteResult);
        replyRepository.save(findReply);
        return replyId;
    }

    // 여러 답글 삭제 : 부모 삭제로 인한 삭제
    public void removeReplyListByParent(List<Reply> replies) {
        for (Reply reply : replies) {
            reply.changeStateToDeleted(new DeleteResult(DeleteResult.Reason.DELETED_BY_PARENT));
        }
        replyRepository.saveAll(replies);
    }

    // 신고에 의한 삭제
    public void removeReplyByReport(Reply reply) {
        DeleteResult deleteResult
                = DeleteResult.builder().deleteReason(DeleteResult.Reason.DELETED_BY_REPORT).build();
        reply.changeStateToDeleted(deleteResult);
        replyRepository.save(reply);
    }

    // 회원 탈퇴로 인한 답글 삭제
    public void removeReplyByDeletedMember(String memberName) {
        DeleteResult deleteResult = DeleteResult.builder()
                .deleteReason(DeleteResult.Reason.DELETED_BY_MEMBER_REMOVE)
                .build();

        List<Reply> replies = replyRepository.findReplyByDeletedMember(memberName);
        for(Reply addDeletedResultByReply : replies) {
            addDeletedResultByReply.changeStateToDeleted(deleteResult);
        }

        replyRepository.saveAll(replies);
    }

    // 수정
    public Reply updateReply(Member loginMember, Long replyId, Reply updateReply) {
        Reply originalReply = checkOwner(loginMember, replyId);
        originalReply.isModifiedNow();
        originalReply.updateContent(updateReply.getContent());
        return replyRepository.save(originalReply);
    }

    // 목록 조회
    public Page<ReplyDto.ReplyListElement> findReplyList(int page, Long commentId, Reply.OrderBy orderBy, Long loginMemberId) {
        Pageable pageable = PageRequest.of(page - 1 , PAGE_ELEMENT_SIZE, Sort.by(orderBy.getTargetProperty()).descending());
        if (loginMemberId == null ) {
            return replyRepository.findReplyListByPage(pageable, commentId);
        } else {
            return replyRepository.findReplyListByPageAndMemberId(pageable, commentId, loginMemberId);
        }
    }

    @Override
    public ThumbDto modifyThumbupState(Long replyId, boolean needInquiry, ThumbDto.ACTION action) {
        Reply findReply = findVerifyReplyById(replyId);
        if (action == ThumbDto.ACTION.PLUS) {
            findReply.plusThumbupCount();
        } else if ( action == ThumbDto.ACTION.MINUS) {
            findReply.minusThumbupCount();
        }
        Reply updateReply = replyRepository.save(findReply);
        if (needInquiry) {
            return updateReply.getThumbCount();
        }
        return null;
    }

    @Override
    public ThumbDto modifyThumbdownState(Long replyId, boolean needInquiry, ThumbDto.ACTION action) {
        Reply findReply = findVerifyReplyById(replyId);
        if (action == ThumbDto.ACTION.PLUS) {
            findReply.plusThumbdownCount();
        } else if ( action == ThumbDto.ACTION.MINUS) {
            findReply.minusThumbdownCount();
        }
        Reply updateReply = replyRepository.save(findReply);
        if (needInquiry) {
            return updateReply.getThumbCount();
        }
        return null;
    }

    // 유효한 Reply 조회
    private Reply findVerifyReplyById(Long replyId) {
        Optional<Reply> findReply = replyRepository.findById(replyId);
        if (findReply.isEmpty()) {
            throw new BusinessLogicException(BusinessExceptionCode.REPLY_NOT_FOUND);
        }
        return findReply.get();
    }

    // 작성자 확인
    private Reply checkOwner(Member loginMember, Long replyId) {
        Reply findReply = findVerifyReplyById(replyId);
        if (!findReply.getWriter().getId().equals(loginMember.getId())) {
            throw new AuthLogicException(AuthExceptionCode.AUTHORIZED_FAIL);
        }
        return findReply;
    }
}
