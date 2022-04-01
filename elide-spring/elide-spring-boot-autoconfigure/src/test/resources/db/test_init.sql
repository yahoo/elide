-- Initialization SQL for the Tests.
-- They are designed to be executed before each test.

-- Create Tables if not present.
CREATE TABLE IF NOT EXISTS Stats (
  id int,
  measure int,
  dimension VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS PlayerStats (
 name varchar(255) not null,
 countryId varchar(255),
 createdOn timestamp,
 updatedOn timestamp,
 highScore bigint,
 primary key (name)
);
CREATE TABLE IF NOT EXISTS PlayerCountry (
  id varchar(255) not null,
  isoCode varchar(255),
  primary key (id)
);

-- Cleanup tables.
DELETE FROM ArtifactVersion;
DELETE FROM ArtifactProduct;
DELETE FROM ArtifactGroup;
DELETE FROM Stats;
DELETE FROM PlayerStats;
DELETE FROM PlayerCountry;
