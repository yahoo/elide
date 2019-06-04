CREATE TABLE IF NOT EXISTS item
    (
      id BIGINT UNSIGNED,
      name VARCHAR(255),
      description VARCHAR(2047),
    ) AS SELECT * FROM CSVREAD('classpath:items.csv');;