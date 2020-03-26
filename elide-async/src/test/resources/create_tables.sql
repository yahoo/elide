CREATE TABLE IF NOT EXISTS AsyncQuery (
    id varchar(255) NOT NULL,
    principalName varchar(255) NOT NULL,
    query varchar(255) NOT NULL,
    queryType varchar(255) NOT NULL,
    status varchar(255) NOT NULL,
    createdOn timestamp NOT NULL,
    updatedOn timestamp NOT NULL,
    naturalkey varchar(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS AsyncQueryResult (
    id varchar(255) NOT NULL,
    contentLength integer NOT NULL,
    responseBody CLOB NOT NULL,
    status integer NOT NULL,
    createdOn timestamp NOT NULL,
    updatedOn timestamp NOT NULL,
    query_id varchar(255) NOT NULL,
    naturalkey varchar(255) NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (query_id) REFERENCES AsyncQuery(id)
);

CREATE TABLE IF NOT EXISTS players
    (
      id BIGINT,
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:player.csv');