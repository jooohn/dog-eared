import ApolloClient from 'apollo-boost';
import fetch from 'node-fetch';
import {
  GetUserIds,
  GetUserIdsQuery,
  GetUserIdsQueryVariables, ImportKindleBookQuotes,
  ImportKindleBookQuotesMutation, ImportKindleBookQuotesMutationVariables
} from '../generated/graphql';

export class DogEaredMain {

  #client: ApolloClient<any>;

  constructor(uri: string) {
    this.#client = new ApolloClient({
      uri,
      fetch: fetch as any,
      request: operation => {
        operation.setContext({
          headers: {
            // TODO
            'x-dog-eared-internal-request-signature': 'dummy',
          },
        });
      },
    });
  }

  async fetchUserIds(): Promise<string[]> {
    const result = await this.#client.query<GetUserIdsQuery, GetUserIdsQueryVariables>({ query: GetUserIds });
    if (result.errors) {
      throw new Error(result.errors.map(e => e.message).join(','));
    }
    return result.data.users.map(({ id }) => id);
  }

  async importKindleQuotedTweets(twitterUserId: string): Promise<void> {
    const result = await this.#client.mutate<ImportKindleBookQuotesMutation, ImportKindleBookQuotesMutationVariables>({
      mutation: ImportKindleBookQuotes,
      variables: {
        twitterUserId,
      }
    });
    if (result.errors) {
      throw new Error(result.errors.map(e => e.message).join(','));
    }
  }

}
