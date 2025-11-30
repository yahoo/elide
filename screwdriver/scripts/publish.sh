#!/usr/bin/env bash
set -e

export GPG_TTY=$(tty)

mkdir -p screwdriver/deploy
chmod 0700 screwdriver/deploy

# Decrypt
openssl aes-256-cbc -pbkdf2 -pass pass:$GPG_ENCPHRASE_2 -in screwdriver/secring.gpg.enc -out screwdriver/deploy/secring-private.asc -d
openssl aes-256-cbc -pbkdf2 -pass pass:$GPG_ENCPHRASE_2 -in screwdriver/pubring.gpg.enc -out screwdriver/deploy/pubring-public.asc -d

# Import keys into gpg-agent
gpg --batch --yes --import screwdriver/deploy/pubring-public.asc
gpg --batch --yes --import screwdriver/deploy/secring-private.asc

# Debug
gpg --list-secret-keys --keyid-format LONG

# Maven deploy
mvn -B deploy -P central -Dmaven.test.skip=true \
    --settings screwdriver/settings/settings-publish.xml \
    -Dgpg.executable=gpg \
    -Dgpg.args="--batch --yes --pinentry-mode loopback"

# Clean up
rm -rf screwdriver/deploy
