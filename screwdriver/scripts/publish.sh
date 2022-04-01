#!/usr/bin/env bash

set -e


export GPG_TTY=$(tty)

mkdir -p screwdriver/deploy
chmod 0700 screwdriver/deploy

openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in screwdriver/pubring.gpg.enc -out screwdriver/deploy/pubring.gpg -d
openssl aes-256-cbc -pass pass:$GPG_ENCPHRASE -in screwdriver/secring.gpg.enc -out screwdriver/deploy/secring.gpg -d

mvn -B deploy -P ossrh -Dmaven.test.skip=true --settings screwdriver/settings/settings-publish.xml

rm -rf screwdriver/deploy
