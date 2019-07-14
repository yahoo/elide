CREATE TABLE IF NOT EXISTS playerStats
    (
      id VARCHAR(255),
      highScore BIGINT,
      lowScore BIGINT,
      overallRating VARCHAR(255),
      country_id VARCHAR(255),
      player_id BIGINT,
      recordedDate DATETIME
    ) AS SELECT * FROM CSVREAD('classpath:player_stats.csv');

CREATE TABLE IF NOT EXISTS country
    (
      id VARCHAR(255),
      isoCode VARCHAR(255),
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:country.csv');

CREATE TABLE IF NOT EXISTS player
    (
      id BIGINT,
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:player.csv');
