#!/bin/sh
VERSION="latest"

(cd doc; make)

rm -rf /tmp/bytebuf-doc/
mkdir -p /tmp/bytebuf-doc/
mv doc/*.html /tmp/bytebuf-doc/

git checkout gh-pages;

rm -rf ./$VERSION
mv /tmp/bytebuf-doc/ ./$VERSION

git add --all ./$VERSION
git commit -a -m "Update ${VERSION} doc"
