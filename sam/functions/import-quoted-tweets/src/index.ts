import { DogEaredMain } from './adapters/dog-eared-main';
import { SQS } from './adapters/sqs';

export const startQuotedTweetsImport = async () => {
  const dogEaredMain = new DogEaredMain(env('GRAPHQL_URI'));
  const sqs = new SQS();
  const queueUrl = env('QUOTED_TWEETS_IMPORT_QUEUE_URL');

  const userIds = await dogEaredMain.fetchUserIds();
  const queueResult = await sqs.sendMessages(queueUrl, userIds.map(userId => ({
    Id: userId,
    MessageBody: JSON.stringify({userId}),
  })));
  if (queueResult.Failed.length !== 0) {
    throw new Error(`Failed to start some: ${queueResult.Failed}`);
  }
};

export const importQuotedTweets = async (event: any, context: any) => {
  console.log({ event, context });
};

function env<T extends string>(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} env must be specified`);
  }
  return value;
}
