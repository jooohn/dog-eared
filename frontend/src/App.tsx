import { ApolloProvider } from '@apollo/react-common';
import ApolloClient from 'apollo-client';
import React from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import { ThemeProvider } from 'theme-ui';
import { bulma as theme } from '@theme-ui/presets';
import { BookRoute } from './route/BookRoute';
import { Home } from './route/Home';
import { UserRoute } from './route/UserRoute';

const App: React.FC<{ client: ApolloClient<any> }> = (props) => {
  return (
    <ApolloProvider client={props.client}>
      <ThemeProvider theme={{
        ...theme,
        breakpoints: ['40em', '60em', '120em'],
      }}>
        <Router>
          <Route path="/" exact>
            <Home/>
          </Route>
          <Route path="/users/:username">
            <UserRoute/>
          </Route>
          <Route path="/books/:bookId">
            <BookRoute/>
          </Route>
        </Router>
      </ThemeProvider>
    </ApolloProvider>
  );
}
export default App;
