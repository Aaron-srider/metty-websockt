#!/bin/bash

pwd

FRONT_END_CODE_PATH="ui"
FRONT_DEPLOY_END_PATH="src/main/resources/static"

# shellcheck disable=SC2164
cd $FRONT_END_CODE_PATH

npm config set registry https://registry.npm.taobao.org
npm install --force

rm -rf dist

npm run build

cd ../
pwd

if [ ! -d "$FRONT_DEPLOY_END_PATH" ]; then
  mkdir -p $FRONT_DEPLOY_END_PATH
fi

# shellcheck disable=SC2115
rm -rf $FRONT_DEPLOY_END_PATH/*

cp -r $FRONT_END_CODE_PATH/dist/* $FRONT_DEPLOY_END_PATH

mvn clean
mvn package -Dmaven.test.skip=true
