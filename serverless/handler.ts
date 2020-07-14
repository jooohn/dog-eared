import { Handler, ScheduledHandler, SQSHandler } from 'aws-lambda';
import 'source-map-support/register';
import { DogEaredMain } from './src/adapters/dog-eared-main';
import { SQS } from './src/adapters/sqs';

type ImportQuotedTweetsRequest = {
  userId: string;
  forceUpdate?: boolean;
}

export const importUser: Handler<{ identity: string }> = async (event, _context) => {
  const dogEaredMain = new DogEaredMain(env('GRAPHQL_URI'));
  const result = await dogEaredMain.importUser(event.identity);
  console.log(`import user ${event.identity} (id=${result})`);
};

export const startQuotedTweetsImport: ScheduledHandler = async (_event, _context) => {
  const dogEaredMain = new DogEaredMain(env('GRAPHQL_URI'));
  const sqs = new SQS();
  const queueUrl = env('IMPORT_QUOTED_TWEETS_QUEUE_URL');

  const userIds = await dogEaredMain.fetchUserIds();
  if (userIds.length === 0) {
    console.log('no users found');
    return;
  }

  const queueResult = await sqs.sendMessages(queueUrl, userIds.map(userId => ({
    Id: userId,
    MessageBody: JSON.stringify({ userId } as ImportQuotedTweetsRequest),
  })));
  if (queueResult.Failed.length !== 0) {
    throw new Error(`Failed to start some: ${queueResult.Failed}`);
  }
};

export const importQuotedTweets: SQSHandler = async (event, _context) => {
  const dogEaredMain = new DogEaredMain(env('GRAPHQL_URI'));
  await Promise.all(event.Records.map(async record => {
    const { userId, forceUpdate } = JSON.parse(record.body) as ImportQuotedTweetsRequest;
    await dogEaredMain.importKindleQuotedTweets(userId, { forceUpdate: forceUpdate!! });
  }))
};

function env(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} env must be specified`);
  }
  return value;
}
