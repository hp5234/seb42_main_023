import React from 'react';
import styled from 'styled-components';
import { BlueBtn } from '../components/common/Btn';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { useEffect, useState, useRef } from 'react';
import { useAppDispatch, useAppSelector } from '../hooks';
import { setNicknameErr } from '../slices/nicknameSlice';

const SetNickname: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const nicknameRef = useRef<HTMLInputElement>(null);
  const nicknameErr = useAppSelector((state) => state.nickname.nicknameErr);

  const [tempName, setTempName] = useState('');

  // 서버는 회원가입이 완료된 유저를 닉네임 설정 페이지로 디라이렉트한다. uri에는 사용자를 임시로 식별할 수 있는 tempName을 담는다.
  useEffect(() => {
    const url = new URL(window.location.href);
    const tempName = url.searchParams.get('tempName') ?? '';
    setTempName(tempName);
  }, []);

  const setNicknameHandler = (): void => {
    const nickname = nicknameRef.current!.value;

    axios
      .post('https://thedragonmoney.com/members/duplicated-name', {
        name: nickname,
        TempName: tempName,
      })
      .then((res) => {
        // 닉네임 중복검사에서 성공하면, login 페이지로 redirect한다.
        // 닉네임 중복검사에서 실패하면, 중복된 닉네임입니다라는 메세지를 띄운다.
        if (res.data.useable) {
          navigate('/login');
        } else {
          // dispatch(changeSetting('중복된 닉네임입니다.'))
          dispatch(setNicknameErr('중복된 닉네임입니다.'));
        }
      })
      .catch((error) =>
        // validation에서 실패했을 경우엔, 아래와 같은 메세지를 띄운다.
        dispatch(setNicknameErr('닉네임은 2자 이상 8자 이하로 작성해주세요.')),
      );
  };

  return (
    <>
      <NicknameFormMain>
        <NicknameForm>
          <div>Logo</div>
          <NicknameInput>
            <label htmlFor="nickname">닉네임</label>
            <input
              id="nickname"
              placeholder="커뮤니티에서 사용할 닉네임을 작성해주세요"
              ref={nicknameRef}
            />
            <p>{nicknameErr}</p>
          </NicknameInput>
          <SignupBtn onClick={setNicknameHandler}>가입하기</SignupBtn>
        </NicknameForm>
      </NicknameFormMain>
    </>
  );
};

export default SetNickname;

// 메인 전체
const NicknameFormMain = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
`;

// 닉네임 폼
const NicknameForm = styled.div`
  width: 503px;
  height: 426px;
  margin: auto;
  background-color: #fcfcfc;
  border: 2px solid #f4f4f4;
  border-radius: 5px;
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  align-items: center;
`;

// 닉네임 입력 label과 input 컨테이너
const NicknameInput = styled.div`
  display: flex;
  flex-direction: column;
  width: 80%;
  > input {
    width: 100%;
    height: 50px;
    margin: 8px 0px;
    padding-left: 4px;
    border: 1px solid #d4d4d4;
    border-radius: 3px;
    color: #7b7b7b;
  }
  > p {
    color: #ca0000;
    font-size: 12px;
  }
`;

// 가입하기 버튼
const SignupBtn = styled(BlueBtn)`
  width: 305px;
  height: 54px;
`;
