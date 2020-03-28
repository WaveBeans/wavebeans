VERSION=$1
CHANGELOG_FILE=../CHANGELOG.md

if [ -z "$VERSION" ]; then
  echo "Please specify version"
  exit 1
fi

CHANGES=$(awk '{print "" $0}' *.md)

mv $CHANGELOG_FILE $CHANGELOG_FILE~

TODAY=$(date "+%Y-%m-%d")

touch $CHANGELOG_FILE

echo "Version $VERSION on $TODAY" >> $CHANGELOG_FILE
echo "------" >> $CHANGELOG_FILE
echo "" >> $CHANGELOG_FILE
echo "$CHANGES" >> $CHANGELOG_FILE
echo "" >> $CHANGELOG_FILE

cat $CHANGELOG_FILE~ >> $CHANGELOG_FILE

rm $CHANGELOG_FILE~
rm *.md