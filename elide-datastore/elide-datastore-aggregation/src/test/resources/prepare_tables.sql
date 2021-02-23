CREATE TABLE IF NOT EXISTS playerStats
    (
      id VARCHAR(255),
      highScore BIGINT,
      lowScore BIGINT,
      overallRating VARCHAR(255),
      country_id VARCHAR(255),
      sub_country_id VARCHAR(255),
      player_id BIGINT,
      player2_id BIGINT,
      recordedDate DATETIME,
      updatedDate DATETIME
    );
TRUNCATE TABLE playerStats;
INSERT INTO playerStats VALUES (1, 1234, 35, 'Good', '840', '840', 1, 2, '2019-07-12 00:00:00', '2019-10-12 00:00:00');
INSERT INTO playerStats VALUES (2, 2412, 241, 'Great', '840', '840', 2, 3, '2019-07-11 00:00:00', '2020-07-12 00:00:00');
INSERT INTO playerStats VALUES (3, 1000, 72, 'Good', '344', '344', 3, 1, '2019-07-13 00:00:00', '2020-01-12 00:00:00');


CREATE TABLE IF NOT EXISTS countries
    (
      id VARCHAR(255),
      iso_code VARCHAR(255),
      name VARCHAR(255),
      continent_id VARCHAR(255),
      nick_name VARCHAR(255),
      un_seats INT
    );
TRUNCATE TABLE countries;
INSERT INTO countries VALUES ('344', 'HKG', 'Hong Kong', '1', NULL, NULL);
INSERT INTO countries VALUES ('840', 'USA', 'United States', '2', 'Uncle Sam', 1);


CREATE TABLE IF NOT EXISTS players
    (
      id BIGINT,
      name VARCHAR(255)
    );
TRUNCATE TABLE players;
INSERT INTO players VALUES (1, 'Jon Doe');
INSERT INTO players VALUES (2, 'Jane Doe');
INSERT INTO players VALUES (3, 'Han');

CREATE TABLE IF NOT EXISTS playerRanking
    (
      id BIGINT,
      ranking BIGINT
    );
TRUNCATE TABLE playerRanking;
INSERT INTO playerRanking VALUES (1, 1);
INSERT INTO playerRanking VALUES (2, 2);
INSERT INTO playerRanking VALUES (3, 3);


CREATE TABLE IF NOT EXISTS videoGames
    (
      game_rounds BIGINT,
      timeSpent BIGINT,
      player_id BIGINT
    );
TRUNCATE TABLE videoGames;
INSERT INTO videoGames VALUES (10, 50, 1);
INSERT INTO videoGames VALUES (20, 150, 1);
INSERT INTO videoGames VALUES (30, 520, 1);
INSERT INTO videoGames VALUES (10, 200, 2);


CREATE TABLE IF NOT EXISTS continents
    (
      id BIGINT,
      name VARCHAR(255)
    );
TRUNCATE TABLE continents;
INSERT INTO continents VALUES (1, 'Asia');
INSERT INTO continents VALUES (2, 'North America');

CREATE TABLE IF NOT EXISTS planets
    (
      id BIGINT,
      name VARCHAR(255)
    );
TRUNCATE TABLE planets;
INSERT INTO planets VALUES (1, 'Mercury');
INSERT INTO planets VALUES (2, 'Venus');
INSERT INTO planets VALUES (3, 'Earth');
