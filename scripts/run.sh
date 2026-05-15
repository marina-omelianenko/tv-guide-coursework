#!/usr/bin/env sh
set -eu

mkdir -p out
javac -encoding UTF-8 -d out \
  src/main/java/ua/course/tvguide/model/Channel.java \
  src/main/java/ua/course/tvguide/model/Program.java \
  src/main/java/ua/course/tvguide/repository/TvGuideRepository.java \
  src/main/java/ua/course/tvguide/web/JsonUtil.java \
  src/main/java/ua/course/tvguide/web/ApiHandler.java \
  src/main/java/ua/course/tvguide/web/StaticFileHandler.java \
  src/main/java/ua/course/tvguide/TvGuideApplication.java

java -cp out ua.course.tvguide.TvGuideApplication "${1:-8080}"
