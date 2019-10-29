CREATE TABLE IF NOT EXISTS playerStats
    (
      id VARCHAR(255),
      highScore BIGINT,
      lowScore BIGINT,
      overallRating VARCHAR(255),
      country_id VARCHAR(255),
      sub_country_id VARCHAR(255),
      player_id BIGINT,
      recordedDate DATETIME
    ) AS SELECT * FROM CSVREAD('classpath:player_stats.csv');

CREATE TABLE IF NOT EXISTS countries
    (
      id VARCHAR(255),
      isoCode VARCHAR(255),
      name VARCHAR(255),
      continent_id VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:country.csv');

CREATE TABLE IF NOT EXISTS players
    (
      id BIGINT,
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:player.csv');

CREATE TABLE IF NOT EXISTS videoGames
    (
      id BIGINT,
      game_rounds BIGINT,
      timeSpent BIGINT
    ) AS SELECT * FROM CSVREAD('classpath:video_games.csv');

CREATE TABLE IF NOT EXISTS continents
    (
      id BIGINT,
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:continents.csv');
