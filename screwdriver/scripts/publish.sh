#!/usr/bin/env bash
set -e

export GPG_TTY=$(tty)

mkdir -p screwdriver/deploy
chmod 0700 screwdriver/deploy

# Decrypt
openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in screwdriver/pubring.gpg.enc -out screwdriver/deploy/pubring.gpg -d
openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in screwdriver/secring.gpg.enc -out screwdriver/deploy/secring.gpg -d

# Import keys into gpg-agent
gpg --batch --yes --import screwdriver/deploy/pubring.gpg
gpg --batch --yes --import screwdriver/deploy/secring.gpg

# Debug
gpg --list-secret-keys --keyid-format LONG

# Maven deploy
mvn -B deploy -P ossrh -Dmaven.test.skip=true \
    --settings screwdriver/settings/settings-publish.xml \
    -Dgpg.executable=gpg \
    -Dgpg.args="--batch --yes --pinentry-mode loopback"

# Clean up
rm -rf screwdriver/deploy
