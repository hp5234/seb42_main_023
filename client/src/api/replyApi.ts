import { apiSlice } from './apiSlice';

// 답글 API
export const repliesApi = apiSlice
  .enhanceEndpoints({ addTagTypes: ['reply'] })
  .injectEndpoints({
    endpoints: (builder) => ({
      // 답글 조회
      getReply: builder.query({
        query: ({ commentId, replyPage }) =>
          `comments/${commentId}/replies?page=${replyPage}&orderby=thumbup`,
        providesTags: ['reply'],
      }),
      // 답글 추가
      setReply: builder.mutation({
        query: ({ commentId, content }) => {
          return {
            url: `comments/${commentId}/replies`,
            method: 'POST',
            body: { commentId, content },
          };
        },
        invalidatesTags: ['reply'],
      }),
      // 답글 수정
      updataReply: builder.mutation({
        query: ({ replyId, content }) => {
          return {
            url: `replies/${replyId}`,
            method: 'PATCH',
            body: { replyId, content },
          };
        },
        invalidatesTags: ['reply'],
      }),
      // 답글 삭제
      deleteReply: builder.mutation({
        query: ({ replyId }) => {
          console.log('id', replyId);
          return {
            url: `replies/${replyId}`,
            method: 'DELETE',
          };
        },
        invalidatesTags: ['reply'],
      }),
      // 답글 좋아요 추가
      addThumbUp: builder.mutation({
        query: ({ replyId }) => {
          return {
            url: `posts/${replyId}/thumbup`,
            method: 'POST',
          };
        },
        invalidatesTags: ['reply'],
      }),
      // 답글 좋아요 제거
      removeThumbUp: builder.mutation({
        query: ({ replyId }) => {
          return {
            url: `posts/${replyId}/thumbup`,
            method: 'DELETE',
          };
        },
        invalidatesTags: ['reply'],
      }),
      // 답글 싫어요 추가
      addThumbDown: builder.mutation({
        query: ({ replyId }) => {
          return {
            url: `posts/${replyId}/thumbdown`,
            method: 'POST',
          };
        },
        invalidatesTags: ['reply'],
      }),
      // 답글 싫어요 제거
      removeThumbDown: builder.mutation({
        query: ({ replyId }) => {
          return {
            url: `posts/${replyId}/thumbdown`,
            method: 'DELETE',
          };
        },
        invalidatesTags: ['reply'],
      }),
    }),
  });
