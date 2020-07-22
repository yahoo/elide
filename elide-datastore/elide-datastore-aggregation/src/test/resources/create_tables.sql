CREATE TABLE IF NOT EXISTS playerStats
    (
      highScore BIGINT,
      lowScore BIGINT,
      overallRating VARCHAR(255),
      country_id VARCHAR(255),
      sub_country_id VARCHAR(255),
      player_id BIGINT,
      player2_id BIGINT,
      recordedDate DATETIME,
      updatedDate DATETIME
    ) AS SELECT * FROM CSVREAD('classpath:player_stats.csv');

CREATE TABLE IF NOT EXISTS countries
    (
      id VARCHAR(255),
      iso_code VARCHAR(255),
      name VARCHAR(255),
      continent_id VARCHAR(255),
      nick_name VARCHAR(255),
      un_seats INT
    ) AS SELECT * FROM CSVREAD('classpath:country.csv');

CREATE TABLE IF NOT EXISTS players
    (
      id BIGINT,
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:player.csv');

CREATE TABLE IF NOT EXISTS videoGames
    (
      game_rounds BIGINT,
      timeSpent BIGINT,
      player_id BIGINT
    ) AS SELECT * FROM CSVREAD('classpath:video_games.csv');

CREATE TABLE IF NOT EXISTS continents
    (
      id BIGINT,
      name VARCHAR(255)
    ) AS SELECT * FROM CSVREAD('classpath:continent.csv');
