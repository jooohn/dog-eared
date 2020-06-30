import React, { useState } from 'react';
import { useParams } from 'react-router';
import { Tweet } from 'react-twitter-widgets';
import { Box, Grid, Heading, Spinner, Text } from 'theme-ui';
import { useUserBookQuotes } from '../api';
import { TweetId, UserId } from '../types';

export const UserBookRoute: React.FC<{ userId: UserId }> = ({ userId }) => {
  const { bookId } = useParams<{ bookId: string }>();
  const result = useUserBookQuotes({ bookId, userId });
  return (
    <>
      {result.type === 'LOADING' && <Spinner/>}
      {result.data && result.data.book ? (
        <>
          <Heading as={'h1'} sx={{fontSize: 8}}>{result.data.book.title}</Heading>
          <Text my={2} sx={{fontSize: 4}}>
            {result.data.book.authors.join(', ')}
          </Text>
          <Grid columns={[1, null, 2, 4]}>
            {result.data.book.userQuotes.map(userQuote => (
              <Box key={userQuote.tweetId}>
                <LoadableTweet tweetId={userQuote.tweetId}/>
              </Box>
            ))}
          </Grid>
        </>
      ) : (
        <div>Book not found</div>
      )}
    </>
  );
};

const LoadableTweet: React.FC<{ tweetId: TweetId }> = ({ tweetId }) => {
  const [loading, setLoading] = useState(true);

  return (
    <>
      {loading && <Spinner/>}
      <Tweet
        tweetId={tweetId}
        onLoad={() => setLoading(false)}
      />
    </>
  )
};
