#!/bin/sh

export JAVA_HOME=$(/usr/libexec/java_home 11)

VERSION=6.1
(cd osb; ../gradlew clean build publishToMavenLocal)
mkdir -p ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
cp ~/.m2/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}/tracker-android-${VERSION}.* ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
