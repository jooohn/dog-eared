import { APIGatewayProxyHandler } from 'aws-lambda';
import { DogEaredMain } from './src/adapters/dog-eared-main';
import { SQS } from './src/adapters/sqs';
import 'source-map-support/register';

export const hello: APIGatewayProxyHandler = async (event, _context) => {
  return {
    statusCode: 200,
    body: JSON.stringify({
      message: 'Go Serverless Webpack (Typescript) v1.0! Your function executed successfully!',
      input: event,
    }, null, 2),
  };
}

export const startQuotedTweetsImport = async () => {
  const dogEaredMain = new DogEaredMain(env('GRAPHQL_URI'));
  const sqs = new SQS();
  const queueUrl = env('IMPORT_QUOTED_TWEETS_QUEUE_URL');

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

function env(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} env must be specified`);
  }
  return value;
}
