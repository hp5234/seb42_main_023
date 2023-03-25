import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { useAppSelector } from '../../hooks';
import LikeIcon from '../../assets/common/LikeIcon';
import TimeIcon from '../../assets/common/TimeIcon';
import ViewIcon from '../../assets/common/ViewIcon';
import Thumnail from '../common/Thumnail';
import { TagItem } from '../common/Tag';
import { Link } from 'react-router-dom';
import { membersPostListApi } from '../../api/memberapi';
import { timeSince } from '../mainP/Timecalculator';
import Pagination from '../common/Pagination';
import { FaBookmark } from 'react-icons/fa';

export interface Tags {
  id: number;
  tagName: string;
}

export interface PostItem {
  postId: number;
  imgUrl: string;
  title: string;
  tags: Tags[];
  memberName: string;
  createdAt: string;
  modified_at: string;
  viewCount: number;
  thumbupCount: number;
}

function MyPostList() {
  const [pageOffset, setPageOffset] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);

  const { query } = useAppSelector(({ header }) => header);
  const membersPostListquery = membersPostListApi.useGetPostListQuery({
    query: query,
    page: currentPage,
  });
  const { data, isSuccess } = membersPostListquery;

  if (!isSuccess) {
    return <div>Loading...</div>;
  }

  return (
    <PostListWrap>
      <List>
        {isSuccess &&
          data.posts.map((post: PostItem) => {
            return (
              <Item key={post.postId}>
                <div>
                  <Link to={`/posts/${post.postId}`}>
                    <Thumnail content={post.imgUrl} />
                  </Link>
                </div>
                <div>
                  <Link to={`/posts/${post.postId}`}>
                    <h1>{post.title}</h1>
                  </Link>
                  <Itemside>
                    <div>
                      {post.tags.map((tag, idx) => (
                        <Tag key={idx}>{tag.tagName}</Tag>
                      ))}
                    </div>
                    <Info>
                      <span>{post.memberName}</span>
                      <span>
                        <TimeIcon />
                        {timeSince(post.createdAt)}
                      </span>
                      <span>
                        <ViewIcon />
                        {post.viewCount}
                      </span>
                      <span>
                        <LikeIcon checked={false} />
                        {post.thumbupCount}
                      </span>
                    </Info>
                  </Itemside>
                </div>
                {isSuccess && (
                  <Bookmark>
                    <FaBookmark />
                  </Bookmark>
                )}
              </Item>
            );
          })}
      </List>
      {isSuccess && (
        <Pagination
          pageInfo={data.pageInfo}
          pageOffset={pageOffset}
          setPageOffset={setPageOffset}
          setCurrentPage={setCurrentPage}
        />
      )}
    </PostListWrap>
  );
}

export default MyPostList;

const List = styled.ul`
  width: 100%;
`;
export const Item = styled.li`
  height: 100px;
  border-bottom: 1px solid var(--border-color);
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 20px;
  h1 {
    font-size: 20px;
    margin-bottom: 4px;
  }
  > div:nth-child(2) {
    flex-grow: 1;
  }
`;
export const Itemside = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  > div {
    display: flex;
  }
`;
export const Info = styled.div`
  span {
    color: var(--sub-font-color);
    font-size: var(--sub-font-size);
    margin-left: 20px;
    flex-direction: row;
    display: flex;
    align-items: center;
  }
`;
export const Tag = styled(TagItem)`
  padding: 4px 10px;
`;
export const PostListWrap = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
`;
const Bookmark = styled.button`
  margin-left: 40px;
  svg {
    color: var(--sub-font-color);
    :hover {
      color: var(--hover-font-gray-color);
    }
  }
`;
