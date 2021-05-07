#!/bin/sh

VERSION=1.0.1
(cd osb; ../gradlew clean build publishToMavenLocal)
mkdir -p ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
cp ~/.m2/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}/tracker-android-${VERSION}.* ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
