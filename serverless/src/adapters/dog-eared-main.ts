import ApolloClient from 'apollo-boost';
import fetch from 'node-fetch';
import {
  GetUserIds,
  GetUserIdsQuery,
  GetUserIdsQueryVariables,
  ImportUser,
  ImportUserMutation,
  ImportUserMutationVariables,
  StartImportKindleBookQuotes,
  StartImportKindleBookQuotesMutation,
  StartImportKindleBookQuotesMutationVariables
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

  async importUser(identity: string): Promise<string> {
    const result = await this.#client.mutate<ImportUserMutation, ImportUserMutationVariables>({
      mutation: ImportUser,
      variables: { identity },
    });
    if (result.errors) {
      throw new Error(result.errors.map(e => e.message).join(','));
    }
    return result.data.importUser;
  }

  async startImportKindleQuotedTweets(twitterUserId: string, options: { forceUpdate: boolean }): Promise<string> {
    const result = await this.#client.mutate<StartImportKindleBookQuotesMutation, StartImportKindleBookQuotesMutationVariables>({
      mutation: StartImportKindleBookQuotes,
      variables: {
        twitterUserId,
        forceUpdate: options.forceUpdate,
      }
    });
    if (result.errors) {
      throw new Error(result.errors.map(e => e.message).join(','));
    }
    return result.data.startImportKindleBookQuotes;
  }

}
