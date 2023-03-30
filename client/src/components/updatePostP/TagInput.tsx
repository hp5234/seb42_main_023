import React, { KeyboardEvent, useEffect, useState } from 'react';
import styled from 'styled-components';
import { useAppDispatch, useAppSelector } from '../../hooks';
import { setTag, setTagContent } from '../../slices/postInputSlice';
import { setTagErr } from '../../slices/validationSlice';
import Tag from '../common/Tag';
import { postsApi } from '../../api/postApi';
import { useParams } from 'react-router-dom';

interface Input {
  className: string;
  placeholder: string;
  onChange: React.KeyboardEvent<HTMLInputElement>;
  value: string;
}

// 공통 컴포넌트
const TagInput: React.FC = () => {
  const dispatch = useAppDispatch();
  const state = useAppSelector((state) => state);
  const valueCheck = (event: React.ChangeEvent<HTMLInputElement>): void => {
    dispatch(setTagContent(event.target.value));
  };
  const params = useParams();
  const postId = params.postId;
  const postQuery = postsApi.useGetPostQuery({ postId });
  const { data } = postQuery;
  const tags = data?.tags;

  //  테그 추가
  const addTagHandler = (event: KeyboardEvent<HTMLInputElement>): void => {
    const tag: Array<string> = state.postInput.tag;
    const tagContent = state.postInput.tagContent;

    // data?.tags.map((tag: string) => {
    //   dispatch(setTag(tag));
    // });

    // 유효성 검사
    if (event.key === 'Enter' && event.nativeEvent.isComposing === false) {
      // 태그 중복 입력 방지
      console.log(tagContent);
      if (tag.includes(tagContent)) {
        dispatch(setTagErr(''));
        return;
      }
      // 공백 방지
      if (tagContent === '') {
        dispatch(setTagErr(''));
        return;
      }
      // 태그 길이
      if (tagContent.length > 10) {
        dispatch(setTagErr('태그 길이는 최대 10 글자 입니다.'));
        return;
      }
      // 띄어쓰기 방지
      if (tagContent.includes(' ')) {
        dispatch(setTagErr('태그에 띄어쓰기를 포함할 수 없습니다.'));
        return;
      }
      // 태그 개수 제한
      if (tag.length >= 5) {
        dispatch(setTagErr('태그는 5개까지만 입력 가능합니다.'));
        dispatch(setTagContent(''));
        dispatch(setTagErr(''));
        return;
      } else {
        dispatch(setTagContent(''));
        dispatch(setTagErr(''));
        dispatch(setTag(tagContent));
      }
    }
  };

  return (
    <>
      {state.validation.tagErr === '' ? (
        <TagInputContainer>
          <Input
            className="tag-input"
            placeholder="태그를 입력하고 엔터를 치세요.(최대 5개)"
            onChange={valueCheck}
            onKeyDown={addTagHandler}
            value={state.postInput.tagContent}
          ></Input>
          <TagConatiner>
            {state.postInput.tag?.map((tag, idx) => {
              return <Tag key={idx} content={tag}></Tag>;
            })}
          </TagConatiner>
        </TagInputContainer>
      ) : (
        <TagInputContainer>
          <Input
            className="tag-input"
            placeholder="태그를 입력하고 엔터를 치세요.(최대 5개)"
            onChange={valueCheck}
            onKeyDown={addTagHandler}
            value={state.postInput.tagContent}
          ></Input>
          <Error>{state.validation.tagErr}</Error>
          <TagConatiner>
            {state.postInput.tag?.map((tag: string, idx: number) => {
              return <Tag key={idx} content={tag}></Tag>;
            })}
          </TagConatiner>
        </TagInputContainer>
      )}
    </>
  );
};

export default TagInput;

const TagInputContainer = styled.div`
  width: 100%;
  height: 40px;
`;

const Input = styled.input`
  width: 100%;
  height: 50px;
  border: 1px solid #d4d4d4;
  padding: 0 10px 0 10px;
  ::placeholder {
    font-style: italic;
  }
`;

const Error = styled.div`
  width: 100%;
  height: 15px;
  color: red;
  margin-top: 10px;
  padding: 0 10px 0 10px;
`;
const TagConatiner = styled.div`
  display: flex;
  width: 1000px;
  justify-content: start;
  margin-top: 15px;
`;
