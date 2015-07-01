#!/bin/bash

# only tagged builds triggers gh-pages pushes
if [ -z $TRAVIS_TAG ]; then
  exit 0
fi

# script inspired by https://gist.github.com/domenic/ec8b0fc8ab45f39403dd

# syncs with gh-pages branch and removes current files
cd target
git init
# config git user and email
git config user.name "Travis-CI"
git config user.email "andreptb+fitnesse-selenium-slim@gmail.com"

# adds apidocs
git add apidocs
git commit -m "Deploy to GitHub Pages generated documentation for $TRAVIS_TAG"
# force push
git push --force --quiet "https://$GH_TOKEN@$GH_REF" master:gh-pages > /dev/null 2>&1
