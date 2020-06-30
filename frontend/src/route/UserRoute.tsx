import React from 'react';
import { useParams } from 'react-router';
import { useRouteMatch, Route, useHistory } from 'react-router-dom';
import { Box, Container, Flex, Grid, Heading, Alert, Spinner, NavLink } from 'theme-ui';
import { useUserAndBooks } from '../api';
import { UserBookRoute } from './UserBookRoute';

const PageAlert: React.FC = ({ children }) => (
  <Alert my={4} p={2}>
    {children}
  </Alert>
);

export const UserRoute: React.FC = () => {
  const history = useHistory();
  const { path, url } = useRouteMatch();
  const { username } = useParams<{ username: string }>();
  const result = useUserAndBooks({ username });
  return (
    <Container p={4}>
      <Grid columns={['320px 1fr']}>
        <Box>
          <Heading mb={4}>@{username}</Heading>
          {result.type === 'LOADING' && <Spinner/>}
          {result.type === 'ERROR' && (
            <PageAlert>
              {result.error.message}
            </PageAlert>
          )}
          {result.data && (
            result.data.user ? (
              <Flex as='nav' style={{flexDirection: 'column'}}>
                {result.data.user.books.map(book => {
                  const target = `${url}/books/${book.id}`;
                  return (
                    <Box key={book.id} my={2}>
                      <NavLink href={target} onClick={(e) => {
                        e.preventDefault();
                        history.push(target)
                      }}>
                        {book.title}
                      </NavLink>
                    </Box>
                  );
                })}
              </Flex>
            ) : (
              <PageAlert>
                User @{username} not found
              </PageAlert>
            )
          )}
        </Box>
        <Box>
          {result.data?.user && (
            <Route path={`${path}/books/:bookId`}>
              <UserBookRoute userId={result.data.user.id} />
            </Route>
          )}
        </Box>
      </Grid>
    </Container>
  )
};

