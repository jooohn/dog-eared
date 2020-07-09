import AWS from 'aws-sdk'
export class SQS {

  #sqs: AWS.SQS;

  constructor() {
    this.#sqs = new AWS.SQS();
  }

  sendMessages(url: string, entries: AWS.SQS.Types.SendMessageBatchRequestEntryList): Promise<AWS.SQS.Types.SendMessageBatchResult> {
    return new Promise((resolve, reject) => {
      const request = {
        QueueUrl: url,
        Entries: entries,
      };
      this.#sqs.sendMessageBatch(request, (err, data) => {
        if (err) {
          return reject(err);
        }
        return resolve(data);
      });
    });
  }

}
