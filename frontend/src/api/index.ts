import { QueryResult } from '@apollo/react-common';
import { ApolloError } from 'apollo-client';
import {
  GetUserAndBooksQuery, GetUserAndBooksQueryVariables,
  GetUserBookQuotesQuery, GetUserBookQuotesQueryVariables,
  useGetUserAndBooksQuery,
  useGetUserBookQuotesQuery
} from '../generated/types';
import { BookId, UserId } from '../types';

type Result<Query> =
  | { type: 'LOADING', data: Query | undefined }
  | { type: 'ERROR', data: Query | undefined, error: ApolloError | Error }
  | { type: 'LOADED', data: Query }

function transformQueryResult<Query, Variables>(result: QueryResult<Query, Variables>): Result<Query> {
  if (result.loading) {
    return { type: 'LOADING', data: result.data };
  }
  if (result.data) {
    return { type: 'LOADED', data: result.data };
  }
  return { type: 'ERROR', data: result.data, error: result.error ?? new Error("Invalid state") };
}

export function useUserAndBooks(variables: GetUserAndBooksQueryVariables): Result<GetUserAndBooksQuery> {
  return transformQueryResult(useGetUserAndBooksQuery({ variables }));
}

export function useUserBookQuotes(variables: GetUserBookQuotesQueryVariables): Result<GetUserBookQuotesQuery> {
  return transformQueryResult(useGetUserBookQuotesQuery({ variables }))
}
