#!/bin/bash -e

function git_branch_name {
    if branch=$(git rev-parse --abbrev-ref HEAD 2> /dev/null); then
        echo "$branch"
    else
        echo ""
    fi
}

function git_revision {
    git rev-list --count HEAD
}

GIT_BRANCH_NAME=`git_branch_name`
echo "GIT_BRANCH_NAME: $GIT_BRANCH_NAME"
GIT_REVISION=`git_revision`
echo "GIT_REVISION: $GIT_REVISION"

if [[ $GIT_BRANCH_NAME == "release/"* ]]; then
    echo "This is a release branch"
    VERSION_NUMBER=`echo "$GIT_BRANCH_NAME" | sed s_release/__g`.$GIT_REVISION
    echo "VERSION_NUMBER: $VERSION_NUMBER"
    mvn versions:set -DnewVersion=$VERSION_NUMBER
else
    echo "This is a snapshot branch. Not modifying version info"
fi

mvn clean deploy
