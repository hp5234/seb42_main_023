import React from 'react';
import styled from 'styled-components';
import { WhiteBtn, BlueBtn } from '../common/Btn';

// interface ReportProps {
//   selectedReport: number | null;
// }

// const Report: React.FC<ReportProps> = ({ selectedReport }) => {
//   console.log(selectedReport); // reportId

const Report: React.FC = () => {
  // console.log(selectedReport); // reportId

  return (
    <ReportContainer>
      <h1>신고내용</h1>
      <Table>
        <tr>
          <th>신고번호</th>
          <td>{singleReportData.reportId}</td>
        </tr>
        <tr className="row-even">
          <th>신고시간</th>
          <td>{singleReportData.reportedAT}</td>
        </tr>
        <tr>
          <th>신고유형</th>
          <td>{singleReportData.reportCategory}</td>
        </tr>
        <tr className="row-even">
          <th>신고대상</th>
          <td>{singleReportData.targetType}</td>
        </tr>
        <tr>
          <th>제목/내용</th>
          <td>{singleReportData.title}</td>
        </tr>
        <tr className="row-even">
          <th>작성자</th>
          <td>{singleReportData.writer}</td>
        </tr>
        <tr>
          <th>신고자</th>
          <td>{singleReportData.reporter}</td>
        </tr>
        <tr className="row-even">
          <th>사유</th>
          <td>{singleReportData.description}</td>
        </tr>
      </Table>
      <div className="button-container">
        <CheckedBtn>확인</CheckedBtn>
        <SeeDetailBtn>자세히 보기</SeeDetailBtn>
      </div>
    </ReportContainer>
  );
};

export default Report;

const ReportContainer = styled.div`
  width: 450px;
  height: max-content;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 1px solid #d4d4d4;
  > h1 {
    font-size: 20px;
  }
  > .button-container {
    display: flex;
    justify-content: center;
  }
`;

const Table = styled.table`
  width: 400px;
  height: max-content;
  margin: 20px 0px;
  border-top: 1px solid #d4d4d4;
  border-bottom: 1px solid #d4d4d4;
  > tr {
    height: 40px;
    &.row-even {
      background-color: #f8f8f8;
    }
    > th {
      font-weight: 600;
      width: 90px;
      /* padding: 0px 10px; */
    }
  }
`;

const CheckedBtn = styled(WhiteBtn)`
  width: 170px;
  height: 40px;
  font-weight: 700;
  margin-right: 8px;
`;

const SeeDetailBtn = styled(BlueBtn)`
  width: 170px;
  height: 40px;
  background-color: #102940;
  font-weight: 700;
  &:hover {
    background-color: #203b53;
  }
`;

const singleReportData = {
  reportId: 1,
  reportedAT: '2019-11-12T16:34:30.388',
  reportCategory: '양리목적/홍보성',
  targetType: '게시글',
  title: '광고. 이 보험 사세요!',
  writer: '작성자',
  reporter: '신고자',
  description:
    '가나다라마바사아자차카타파하이게스무글자가나다라마바사아자차카타파하이게마흔셋글자ㅎ',
  postId: 2,
  commentId: null,
  replyId: null,
};
