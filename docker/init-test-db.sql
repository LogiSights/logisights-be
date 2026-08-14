-- Runs once when the postgres container's data volume is first created.
-- Provisions a separate database for the test datasource (see application.properties
-- %test.quarkus.datasource.jdbc.url) so tests never run against the dev database.
CREATE DATABASE logisights_test OWNER logisights;
