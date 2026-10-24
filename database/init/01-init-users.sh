#!/bin/sh
set -eu

JAVA_DB_USER="$(cat /run/credentials/java_user)"
JAVA_DB_PASS="$(cat /run/credentials/java_password)"
PYTHON_DB_USER="$(cat /run/credentials/python_user)"
PYTHON_DB_PASS="$(cat /run/credentials/python_password)"
export MYSQL_PWD="$(cat /run/credentials/root_password)"

mysql -uroot <<EOSQL
CREATE DATABASE IF NOT EXISTS BUCTTA_DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '${JAVA_DB_USER}'@'%' IDENTIFIED BY '${JAVA_DB_PASS}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, EXECUTE, SHOW VIEW
    ON BUCTTA_DATABASE.* TO '${JAVA_DB_USER}'@'%';
CREATE USER IF NOT EXISTS '${PYTHON_DB_USER}'@'%' IDENTIFIED BY '${PYTHON_DB_PASS}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP, REFERENCES
    ON BUCTTA_DATABASE.* TO '${PYTHON_DB_USER}'@'%';
FLUSH PRIVILEGES;
EOSQL
