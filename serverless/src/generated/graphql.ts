import gql from 'graphql-tag';
export type Maybe<T> = T | null;
export type Exact<T extends { [key: string]: any }> = { [K in keyof T]: T[K] };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  Unit: any;
};

export type BookZio = {
  __typename?: 'BookZIO';
  id: Scalars['ID'];
  title: Scalars['String'];
  url: Scalars['String'];
  authors: Array<Scalars['String']>;
  quotes?: Maybe<Array<QuoteZio>>;
  userQuotes?: Maybe<Array<QuoteZio>>;
};


export type BookZioUserQuotesArgs = {
  value: Scalars['ID'];
};

export type MutationsZio = {
  __typename?: 'MutationsZIO';
  importUser?: Maybe<Scalars['ID']>;
  importKindleBookQuotes?: Maybe<Scalars['Unit']>;
};


export type MutationsZioImportUserArgs = {
  identity: Scalars['ID'];
};


export type MutationsZioImportKindleBookQuotesArgs = {
  twitterUserId: Scalars['ID'];
  forceUpdate?: Maybe<Scalars['Boolean']>;
};

export type QueriesZio = {
  __typename?: 'QueriesZIO';
  user?: Maybe<UserZio>;
  book?: Maybe<BookZio>;
  users: Array<UserZio>;
};


export type QueriesZioUserArgs = {
  value: Scalars['ID'];
};


export type QueriesZioBookArgs = {
  value: Scalars['ID'];
};

export type QuoteZio = {
  __typename?: 'QuoteZIO';
  tweetId: Scalars['ID'];
  url: Scalars['String'];
  body: Scalars['String'];
  user?: Maybe<UserZio>;
  book?: Maybe<BookZio>;
};


export type UserZio = {
  __typename?: 'UserZIO';
  id: Scalars['ID'];
  username: Scalars['ID'];
  books?: Maybe<Array<BookZio>>;
  quotes?: Maybe<Array<QuoteZio>>;
  bookQuotes?: Maybe<Array<QuoteZio>>;
};


export type UserZioBookQuotesArgs = {
  value: Scalars['ID'];
};

export type ImportKindleBookQuotesMutationVariables = Exact<{
  twitterUserId: Scalars['ID'];
  forceUpdate: Scalars['Boolean'];
}>;


export type ImportKindleBookQuotesMutation = (
  { __typename?: 'MutationsZIO' }
  & Pick<MutationsZio, 'importKindleBookQuotes'>
);

export type ImportUserMutationVariables = Exact<{
  identity: Scalars['ID'];
}>;


export type ImportUserMutation = (
  { __typename?: 'MutationsZIO' }
  & Pick<MutationsZio, 'importUser'>
);

export type GetUserIdsQueryVariables = Exact<{ [key: string]: never; }>;


export type GetUserIdsQuery = (
  { __typename?: 'QueriesZIO' }
  & { users: Array<(
    { __typename?: 'UserZIO' }
    & Pick<UserZio, 'id'>
  )> }
);


export const ImportKindleBookQuotes = gql`
    mutation ImportKindleBookQuotes($twitterUserId: ID!, $forceUpdate: Boolean!) {
  importKindleBookQuotes(twitterUserId: $twitterUserId, forceUpdate: $forceUpdate)
}
    `;
export const ImportUser = gql`
    mutation ImportUser($identity: ID!) {
  importUser(identity: $identity)
}
    `;
export const GetUserIds = gql`
    query GetUserIds {
  users {
    id
  }
}
    `;