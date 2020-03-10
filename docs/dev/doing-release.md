How to do a release?
========

0. Assumptions:

    * Every branch/feature pushes the description `.md` file into `.release` directory. Every new line will be prepended with `*`.

1. Locally:

    1. Merge everything needed into `develop` branch. Run tests.
    2. Update changelog using script `.release/updateChangelog.sh` specifying the version as a parameter. You need to be in `.release` directory to do that.
        * Also update the release log with extra information coming not from branches.
        * Change the order of the entries if necessary. 
    3. Update the root `README.md` file with the link to new version on Bintray.
    4. Update `version` in `gradle.properties`.
    5. Commit with message `Release $VERSION`.
    6. Tag the current commit and push it.

2. Bintray:
    1. Upload new version:
    ```bash
     ./gradlew bintrayUpload -Pversion=$VERSION -Pbintray.user=$USER -Pbintray.key=$KEY
    ```
    2. Goto bintray.com, choose the wavebeans organization and publish artifacts.
    3. Upload new version of cli tool:
        * build locally via `./gradlew distTar distZip`
        * on Bintray, select version, then `Actions > Upload Files`. Upload `cli/build/distributions/*.*`

3. GitHub:
    * Create the release with changes from changelog based on the pushed tag.

4. Merge tag to master.

5. Website `wavebeans.io`:
    1. Update documentation
    2. Update release notes: https://wavebeans.io/wavebeans/release_notes.html
        * Update links from `/docs/user/lib` to `/docs/api`
        * Update links from `/docs/user/http` to `/docs/http`
        * Update links from `/docs/user/cli` to `/docs/cli`
        * Update links from `/docs/user/exe` to `/docs/exe`
        * `.md` to `.html`

6. Social channels:
    * Post the link on twitter and telegram channel.