#!/bin/bash

#immediately exit script with an error if a command fails
set -euo pipefail

export COPY_EXTENDED_ATTRIBUTES_DISABLE=true
export COPYFILE_DISABLE=true

INPUT_FILE=$1
EXPLODED=$2.exploded
BACKUP_JMODS=$2.backup
USERNAME=$3
PASSWORD=$4
CODESIGN_STRING=$5
NOTARIZE=$6
BUNDLE_ID=$7

cd "$(dirname "$0")"

function log() {
  echo "$(date '+[%H:%M:%S]') $*"
}

log "Deleting $EXPLODED ..."
if test -d "$EXPLODED"; then
  find "$EXPLODED" -mindepth 1 -maxdepth 1 -exec chmod -R u+wx '{}' \;
fi
rm -rf "$EXPLODED"
mkdir "$EXPLODED"
rm -rf "$BACKUP_JMODS"
mkdir "$BACKUP_JMODS"

log "Unzipping $INPUT_FILE to $EXPLODED ..."
tar -xzvf "$INPUT_FILE" --directory $EXPLODED
rm "$INPUT_FILE"
BUILD_NAME="$(ls "$EXPLODED")"
if test -d $EXPLODED/$BUILD_NAME/Contents/Home/jmods; then
  mv $EXPLODED/$BUILD_NAME/Contents/Home/jmods $BACKUP_JMODS
fi
if test -d $EXPLODED/$BUILD_NAME/Contents/Home/Frameworks; then
  mv $EXPLODED/$BUILD_NAME/Contents/Home/Frameworks $BACKUP_JMODS
fi
if test -f $EXPLODED/$BUILD_NAME/Contents/MacOS/libjli.dylib; then
  mv $EXPLODED/$BUILD_NAME/Contents/MacOS/libjli.dylib $BACKUP_JMODS
fi

#log "$INPUT_FILE unzipped and removed"
log "$INPUT_FILE extracted and removed"

APPLICATION_PATH="$EXPLODED/$BUILD_NAME"

find "$APPLICATION_PATH/Contents/Home/bin" \
  -maxdepth 1 -type f -name '*.jnilib' -print0 |
  while IFS= read -r -d $'\0' file; do
    if [ -f "$file" ]; then
      log "Linking $file"
      b="$(basename "$file" .jnilib)"
      ln -sf "$b.jnilib" "$(dirname "$file")/$b.dylib"
    fi
  done

find "$APPLICATION_PATH/Contents/" \
  -maxdepth 1 -type f -name '*.txt' -print0 |
  while IFS= read -r -d $'\0' file; do
    if [ -f "$file" ]; then
      log "Moving $file"
      mv "$file" "$APPLICATION_PATH/Contents/Resources"
    fi
  done

non_plist=$(find "$APPLICATION_PATH/Contents/" -maxdepth 1 -type f -and -not -name 'Info.plist' | wc -l)
if [[ $non_plist -gt 0 ]]; then
  log "Only Info.plist file is allowed in Contents directory but found $non_plist file(s):"
  log "$(find "$APPLICATION_PATH/Contents/" -maxdepth 1 -type f -and -not -name 'Info.plist')"
  exit 1
fi

log "Unlocking keychain..."
# Make sure *.p12 is imported into local KeyChain
security unlock-keychain -p "$PASSWORD" "/Users/$USERNAME/Library/Keychains/login.keychain"

attempt=1
limit=3
set +e
while [[ $attempt -le $limit ]]; do
  log "Signing (attempt $attempt) $APPLICATION_PATH ..."
  ./sign.sh "$APPLICATION_PATH" "$CODESIGN_STRING"
  ec=$?
  if [[ $ec -ne 0 ]]; then
    ((attempt += 1))
    if [ $attempt -eq $limit ]; then
      set -e
    fi
    log "Signing failed, wait for 30 sec and try to sign again"
    sleep 30
  else
    log "Signing done"
    codesign -v "$APPLICATION_PATH" -vvvvv
    log "Check sign done"
    ((attempt += limit))
  fi
done

set -e

if [ "$NOTARIZE" = "yes" ]; then
  log "Notarizing..."
  # shellcheck disable=SC1090
  source "$HOME/.notarize_token"
  APP_NAME=$(echo ${INPUT_FILE} | awk -F"." '{ print $1 }')
  # Since notarization tool uses same file for upload token we have to trick it into using different folders, hence fake root
  # Also it leaves copy of zip file in TMPDIR, so notarize.sh overrides it and uses FAKE_ROOT as location for temp TMPDIR
  FAKE_ROOT="$(pwd)/fake-root"
  mkdir -p "$FAKE_ROOT"
  echo "Notarization will use fake root: $FAKE_ROOT"
  ./notarize.sh "$APPLICATION_PATH" "$APPLE_USERNAME" "$APPLE_PASSWORD" "$APP_NAME" "$BUNDLE_ID" "$FAKE_ROOT"
  rm -rf "$FAKE_ROOT"

  set +e
  log "Stapling..."
  xcrun stapler staple "$APPLICATION_PATH"
else
  log "Notarization disabled"
  log "Stapling disabled"
fi

log "Zipping $BUILD_NAME to $INPUT_FILE ..."
(
  #cd "$EXPLODED"
  #ditto -c -k --sequesterRsrc --keepParent "$BUILD_NAME" "../$INPUT_FILE"
  if test ! -z $(ls $BACKUP_JMODS/libjli.dylib); then
    mv $BACKUP_JMODS/libjli.dylib $EXPLODED/$BUILD_NAME/Contents/MacOS
  fi
  if test -d $BACKUP_JMODS/jmods; then
    mv $BACKUP_JMODS/jmods $EXPLODED/$BUILD_NAME/Contents/Home
  fi
  if test -d $BACKUP_JMODS/Frameworks; then
    mv $BACKUP_JMODS/Frameworks $EXPLODED/$BUILD_NAME/Contents/Home
  fi

  COPYFILE_DISABLE=1 tar -pczf $INPUT_FILE --exclude='*.dSYM' --exclude='man' -C $EXPLODED $BUILD_NAME
  log "Finished zipping"
)
rm -rf "$EXPLODED"
log "Done"
