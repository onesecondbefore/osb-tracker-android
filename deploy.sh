#!/bin/sh

export JAVA_HOME=$(/usr/libexec/java_home 17)

VERSION=7.4
(cd osb; ../gradlew clean build)
(cd osb; ../gradlew publishToMavenLocal)
mkdir -p ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
cp ~/.m2/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}/tracker-android-${VERSION}.* ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
