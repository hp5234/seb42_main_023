import React, { useRef } from 'react';
import { useParams } from 'react-router-dom';
import styled from 'styled-components';
import { commentsApi } from '../../api/commentApi';
import { useAppDispatch } from '../../hooks';
import { addCommentEdit, setComment } from '../../slices/commentSlice';

const CommentInput: React.FC = () => {
  const commentRef = useRef<HTMLInputElement>(null);
  const dispatch = useAppDispatch();
  const params = useParams();
  const postId = params.postId;
  const query = commentsApi.useGetCommentQuery({ postId });
  // 댓글 추가 mutation
  const commetMutation = commentsApi.useSetCommentMutation();
  const [setComments] = commetMutation;

  // 댓글 추가
  const addCommentHandler = async () => {
    console.log('test');
    await setComments({
      postId: postId,
      content: commentRef.current?.value,
    });
    dispatch(addCommentEdit(false));
    commentRef.current!.value = '';
  };

  const valueCheck = (event: React.ChangeEvent<HTMLInputElement>): void => {
    dispatch(setComment(event.target.value));
  };

  const enterHandler = (event: React.KeyboardEvent<HTMLInputElement>): void => {
    if (!commentRef.current?.value) return;
    if (event.key === 'Enter' && event.nativeEvent.isComposing === false) {
      addCommentHandler();
    }
  };

  return (
    <CommentInputContainer>
      <h1>댓글 {query.data && query.data.comment.length}개 </h1>
      <Input
        type="text"
        placeholder="댓글을 남겨 주세요"
        ref={commentRef}
        onChange={valueCheck}
        onKeyDown={enterHandler}
      ></Input>
      <AddCommentBtn onClick={addCommentHandler}>등록</AddCommentBtn>
    </CommentInputContainer>
  );
};

export default CommentInput;

const CommentInputContainer = styled.div`
  position: relative;
  width: 720px;
  height: 100px;
  margin-top: 30px;
  h1 {
    font-size: 24px;
    font-weight: 400;
  }
`;
const Input = styled.input`
  width: 720px;
  height: 50px;
  border: 1px solid #d4d4d4;
  padding: 0 10px 0 10px;
`;
const AddCommentBtn = styled.button`
  width: 50px;
  height: 30px;
  padding: 0 10px 0 10px;
  background-color: white;
  position: absolute;
  top: 55px;
  right: 15px;
  color: gray;
  cursor: pointer;
`;
