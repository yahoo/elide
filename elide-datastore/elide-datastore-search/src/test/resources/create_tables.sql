CREATE TABLE IF NOT EXISTS item
    (
      id BIGINT,
      name VARCHAR(255),
      description VARCHAR(2047),
      modifiedDate TIMESTAMP,
      price BIGINT
    ) AS SELECT * FROM CSVREAD('classpath:items.csv');
