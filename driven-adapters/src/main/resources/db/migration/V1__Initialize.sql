CREATE TABLE kindle_books (
    id              VARCHAR(32) NOT NULL PRIMARY KEY,
    title           TEXT        NOT NULL,
    slug            TEXT        NOT NULL UNIQUE,
    authors         TEXT[]      NOT NULL,
    url             TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE twitter_users (
    id          VARCHAR(32) NOT NULL PRIMARY KEY,
    username    VARCHAR(15) NOT NULL UNIQUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE kindle_quoted_tweets (
    tweet_id        VARCHAR(32) NOT NULL PRIMARY KEY,
    twitter_user_id VARCHAR(32) NOT NULL REFERENCES twitter_users(id),
    book_id         VARCHAR(32) NOT NULL REFERENCES kindle_books(id),
    quote_url       TEXT        NOT NULL,
    quote_body      TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ON kindle_quoted_tweets (twitter_user_id);
CREATE INDEX ON kindle_quoted_tweets (book_id);

CREATE TABLE processed_tweets (
    twitter_user_id         VARCHAR(32) NOT NULL PRIMARY KEY REFERENCES twitter_users(id),
    last_processed_tweet_id VARCHAR(32) NOT NULL,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
)
