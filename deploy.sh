#!/bin/sh

VERSION=1.0.0
(cd osb; ../gradlew clean build publishToMavenLocal)
cp ~/.m2/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}/tracker-android-${VERSION}.* ../onesecondbefore.com/public/repository/com/onesecondbefore/tracker/tracker-android/${VERSION}
