# RedditSlideshow

RedditSlideshow is a small Reddit image slideshow.

# Usage

## Building

`./gradlew shadow` or `gradlew.bat shadow`

## Running

`java -jar ./server/build/libs/server-x.jar`

## Authentication

A login page is available at `/login`.availiable

# Environmental Variables

A few env vars need to be present to run.

## PORT

The port that will be used for web traffic

## REDDIT_CLIENT_ID
Deprecated

App client ID from Reddit

## REDDIT_CLIENT_SECRET
Deprecated

App secret from Reddit

## REDIS_URL

Redis URL string