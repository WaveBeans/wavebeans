# to run locally
# assuming `docker login` is perfomed

ORG=alexeysubbotin
REPO=wavebeans
FINAL=false

PROJECT_ROOT="../.."
VERSION=`cat $PROJECT_ROOT/gradle.properties | grep version | tr -d 'version='`
GIT_SHORT=`git rev-parse --short HEAD`

if [ "$FINAL" == "true" ]; then
  IMAGE=$ORG/$REPO:$VERSION
else
  IMAGE=$ORG/$REPO:$VERSION-$GIT_SHORT
fi

# prebuilt artifact
echo ">> Prebuilding project"
CURR_DIR=`pwd`
cd $PROJECT_ROOT

./gradlew :distr:distTar
cd $CURR_DIR
echo ">> OK"

# prepare resources
echo ">> Preparing Resources"
mkdir -p distr
cp $PROJECT_ROOT/distr/build/distributions/wavebeans-$VERSION.tar distr/
echo ">> OK"

# build an image
echo ">> Building image '$IMAGE' with version of binaries '$VERSION'"
docker build . -t $IMAGE --build-arg VERSION=$VERSION
echo ">> OK"

if [ "$1" == "andPush" ]; then
  docker push $IMAGE
fi