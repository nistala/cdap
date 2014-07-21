#!/usr/bin/env bash

# Build script for Reactor docs
# Builds the docs
# Copies the javadocs into place
# Zips everything up so it can be staged

DATE_STAMP=`date`
SCRIPT=`basename $0`

SOURCE="source"
BUILD="build"
BUILD_PDF="build-pdf"
HTML="html"
# APIDOCS="apidocs"
APIDOCS="javadoc"
JAVADOCS="javadocs"
LICENSES="licenses"
LICENSES_PDF="licenses-pdf"
EXAMPLES="examples"
DOWNLOADS="_downloads"

EXAMPLE_PVA="continuuity-PageViewAnalytics-*.zip"
EXAMPLE_RCA="continuuity-ResponseCodeAnalytics-*.zip"
EXAMPLE_TA="continuuity-TrafficAnalytics-*.zip"

SCRIPT_PATH=`pwd`
SOURCE_PATH="$SCRIPT_PATH/$SOURCE"
BUILD_PATH="$SCRIPT_PATH/$BUILD"
HTML_PATH="$BUILD_PATH/$HTML"

DOCS_PY="$SCRIPT_PATH/../tools/scripts/docs.py"

REST_SOURCE="$SOURCE_PATH/rest.rst"
REST_PDF="$SCRIPT_PATH/$BUILD_PDF/rest.pdf"

INSTALL_GUIDE="$SCRIPT_PATH/../install-guide"
INSTALL_SOURCE="$INSTALL_GUIDE/source/install.rst"

WWW_PATH="/var/www/website-docs/reactor"

VERSION_TXT="version.txt"

RELEASE=""
# Comment out the next line for SNAPSHOT builds
RELEASE="-PfinalRelease=true"

if [ "x$2" == "x" ]; then
  REACTOR_PATH="$SCRIPT_PATH/../.."
else
  REACTOR_PATH="$2"
fi
# REACTOR_JAVADOCS="$REACTOR_PATH/continuuity-api/target/site/apidocs"

REACTOR_DIST="$REACTOR_PATH/distributions/build/distributions"
REACTOR_JAVADOCS="$REACTOR_PATH/distributions/build/javadoc"

ZIP_FILE_NAME=$HTML
ZIP="$ZIP_FILE_NAME.zip"
STAGING_SERVER="stg-web101.sw.joyent.continuuity.net"

function usage() {
  cd $REACTOR_PATH
  REACTOR_PATH=`pwd`
  echo "Build script for Reactor docs"
  echo "Usage: $SCRIPT < option > "
  echo ""
  echo "  Options (select one)"
  echo "    build        Clean build of javadocs, docs (HTML and PDF), copy javadocs and pdfs, zip results"
  echo "    stage        Stages and logins to server"
  echo "  or "
  echo "    build-docs   Clean build of docs"
  echo "    javadocs     Clean build of javadocs"
  echo "    pdf-rest     Clean build of REST PDF"
  echo "    pdf-install  Clean build of Install Guide PDF"
  echo "    login        Logs you into $STAGING_SERVER"
  echo "    reactor      Path to Reactor source for javadocs, if not $REACTOR_PATH"
  echo "    zip          Zips docs into $ZIP"
  echo "  or "
  echo "    depends      Build Site listing dependencies"
  echo "    sdk          Build SDK"
  echo " "
  exit 1
}

function clean() {
  rm -rf $SCRIPT_PATH/$BUILD
}

# function build_javadocs() {
#   cd $REACTOR_PATH
# #   mvn clean package site -pl continuuity-api -am -Pjavadocs -DskipTests
#   ./gradlew clean docs_dist
# }

function build_docs() {
#   sphinx-build -b html -d build/doctrees source build/html
  cd $REACTOR_PATH
  ./gradlew clean
  ./gradlew docs_dist $RELEASE
  # Remember: gradle builds the docs directly into build!
}

function build_example_zips() {
  cd $REACTOR_PATH/examples
  mvn -DskipTests clean package 
  cd $REACTOR_PATH
  ./gradlew examples_response_analytics $RELEASE
  ./gradlew examples_traffic_analytics $RELEASE
  ./gradlew examples_page_analytics $RELEASE
}

function copy_javadocs() {
  version
  cd $BUILD_PATH
  rm -rf $JAVADOCS
# I don't have Java 6 and can't use the built javadocs: 
#   cp -r $REACTOR_JAVADOCS .
#   mv -f $APIDOCS $JAVADOCS
# Instead: copy from downloaded SDK:
  DOWNLOAD_SDK_JAVADOCS="${HOME}/Downloads/continuuity-sdk-$REACTOR_VERSION/javadocs"
  echo "Using DOWNLOAD_SDK_JAVADOCS: $DOWNLOAD_SDK_JAVADOCS"
  cp -r $DOWNLOAD_SDK_JAVADOCS .
}

function copy_license_pdfs() {
  cd $BUILD_PATH
  rm -rf $LICENSES
  cp -r $SCRIPT_PATH/$LICENSES_PDF .
  mv -f $LICENSES_PDF $LICENSES
}

function copy_example_zips() {
  cd $BUILD_PATH/$DOWNLOADS
  rm -rf *
  cp -r $REACTOR_DIST/$EXAMPLE_PVA .
  cp -r $REACTOR_DIST/$EXAMPLE_RCA .
  cp -r $REACTOR_DIST/$EXAMPLE_TA .
}

function move_build_to_html() {
  cd $SCRIPT_PATH
  rm -rf $HTML
  mv $BUILD $HTML
  mkdir $BUILD
  mv $HTML $BUILD/$HTML
}

function make_zip() {
  cd $SCRIPT_PATH/$BUILD
  zip -r $ZIP_FILE_NAME $HTML/*
}

function build_pdf_rest() {
#   version # version is not needed because the renaming is done by the pom.xml file
  rm -rf $SCRIPT_PATH/$BUILD_PDF
  mkdir $SCRIPT_PATH/$BUILD_PDF
  python $DOCS_PY -g pdf -o $REST_PDF $REST_SOURCE
}

function build_pdf_install() {
  version
  INSTALL_PDF="$INSTALL_GUIDE/$BUILD_PDF/Reactor-Installation-Guide-v$REACTOR_VERSION.pdf"
  rm -rf $INSTALL_GUIDE/$BUILD_PDF
  mkdir $INSTALL_GUIDE/$BUILD_PDF
  python $DOCS_PY -g pdf -o $INSTALL_PDF $INSTALL_SOURCE
}


function stage_docs() {
  echo "Deploying..."
  echo "rsync -vz $SCRIPT_PATH/$BUILD/$ZIP \"$USER@$STAGING_SERVER:$ZIP\""
  rsync -vz $SCRIPT_PATH/$BUILD/$ZIP "$USER@$STAGING_SERVER:$ZIP"
  version
  cd_cmd="cd $WWW_PATH; ls"
  remove_cmd="sudo rm -rf $REACTOR_VERSION"
  unzip_cmd="sudo unzip ~/$ZIP; sudo mv $HTML $REACTOR_VERSION"
  echo ""
  echo "To install on server:"
  echo ""
  echo "  $cd_cmd"
  echo "  $remove_cmd; ls"
  echo "  $unzip_cmd; ls"
  echo ""
  echo "or, on one line:"
  echo ""
  echo "  $cd_cmd; $remove_cmd; ls; $unzip_cmd; ls"
  echo ""
  echo "or, using current branch:"
  echo ""
  echo "  $cd_cmd"
  echo "  $remove_cmd-$GIT_BRANCH; ls"
  echo "  $unzip_cmd-$GIT_BRANCH; ls"
  echo ""
  login_staging_server
}

function login_staging_server() {
  echo "Logging into:"
  echo "ssh \"$USER@$STAGING_SERVER\""
  ssh "$USER@$STAGING_SERVER"
}

function build() {
  clean
  build_docs
  build_example_zips
  copy_javadocs
  copy_license_pdfs
  copy_example_zips
  move_build_to_html
  make_zip
}

function build_sdk() {
  build_pdf_rest
  cd $REACTOR_PATH
  mvn clean package -DskipTests -P examples && mvn package -pl singlenode -am -DskipTests -P dist,release
}

function build_dependencies() {
  cd $REACTOR_PATH
  mvn clean package site -am -Pjavadocs -DskipTests
}

function version() {
  REACTOR_VERSION=$(cat $REACTOR_PATH/$VERSION_TXT)
  IFS=/ read -a branch <<< "`git rev-parse --abbrev-ref HEAD`"
  GIT_BRANCH="${branch[1]}"
}

function print_version() {
  version
  echo "REACTOR_VERSION: $REACTOR_VERSION"
  echo "GIT_BRANCH: $GIT_BRANCH"
  echo "REACTOR_JAVADOCS: $REACTOR_JAVADOCS"
  echo "BUILD_PATH: $BUILD_PATH"
}
    
if [ $# -lt 1 ]; then
  usage
  exit 1
fi

case "$1" in
  build )             build; exit 1;;
  build-docs )        build_docs; exit 1;;
  build_example_zips ) build_example_zips; exit 1;;
  copy_javadocs )     copy_javadocs; exit 1;;
  copy_example_zips ) copy_example_zips; exit 1;;
  copy_license_pdfs ) copy_license_pdfs; exit 1;;
  javadocs )          build_javadocs; exit 1;;
  depends )           build_dependencies; exit 1;;
  login )             login_staging_server; exit 1;;
  pdf-install )       build_pdf_install; exit 1;;
  pdf-rest )          build_pdf_rest; exit 1;;
  sdk )               build_sdk; exit 1;;
  stage )             stage_docs; exit 1;;
  version )           version; exit 1;;
  print_version )     print_version; exit 1;;
  zip )               make_zip; exit 1;;
  * )                 usage; exit 1;;
esac
