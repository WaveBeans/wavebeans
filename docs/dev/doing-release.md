# How to do a release?

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->


- [What this document is about](#what-this-document-is-about)
- [Release steps](#release-steps)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## What this document is about

This document describe the process of making releases. Some of this position may be automated but currently is not done.

## Release steps

0. Assumptions:

    * Every branch/feature pushes the description `.md` file into `.release` directory.

1. Locally:

    1. Merge everything needed into `develop` branch. Run tests.
    2. Update changelog using script `.release/updateChangelog.sh` specifying the version as a parameter. You need to be in `.release` directory to do that.
        * Also update the release log with extra information coming not from branches.
        * Change the order of the entries if necessary. 
    3. Update the root `README.md` file with the link to new version on Bintray.
    4. Update `version` in `gradle.properties`.
    5. Commit with message `Release $VERSION`.
    6. Tag the current commit and push it.

2. Push to remote:
    
    1. Upload new version to Maven Central:
       * Build with gradle:
            ```bash
             ./gradlew publish -Psigning.keyId=<LAST_8_SYMBOLS_OF_KEY_ID> -Psigning.password=<KEY_PASSWORD> -Psigning.secretKeyRingFile=<PATH_TO_SECRET_KEY_RING_FILE> -Pmaven.user=<OSSRH_USER> -Pmaven.key=<OSSRH_PASSWORD>
            ```
       * [Commit the release](https://central.sonatype.org/publish/release/):
            * Go to [Staging Repositories on Sonatype](https://s01.oss.sonatype.org/#stagingRepositories), use your OSSRH credentials to log in
            * Select the `iowavebeans-XXXX` repository and click close.
            * Verify if everything is correct on `Activity` tab, correct if required.
            * Press `Release` to commit changes.
       * If doing it for the first time, you need to set up a few things:
            * Publishing
                *  You need to create a JIRA account with [OSSRH](https://central.sonatype.org/publish/publish-guide/#initial-setup)
            * Signing
                * [install `gpg` utility](https://central.sonatype.org/publish/requirements/gpg/)
                * Generate a key pair:
                    ```bash
                     gpg --full-generate-key
                    ```
                    *Select the RSA algorithm with 3072 key length*
                * Export secret key ring file:
                    ```shell
                    gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg
                    ```
                *  Upload a public key:
                    ```bash
                   gpg --keyserver hkp://pool.sks-keyservers.net --send-keys <KEY_ID_HERE>
                    ```
                   To get the key ID you can list all the keys via:
                    ```bash
                    gpg --list-keys
                    ```
    3. Upload new version of the tool:
        * build locally via `./gradlew :distr:distTar :distr:distZip`
        * on Bintray, select version, then `Actions > Upload Files`. Upload `distr/build/distributions/*.*`

3. GitHub:
    * Create the release with changes from changelog based on the pushed tag.
    * Add the link to the version on BinTray.

4. Merge tag to master.

5. Website `wavebeans.io`:
    1. Update documentation
        ```bash
       cd docs-build
       ./build.sh
        ```
    2. Update release notes: https://wavebeans.io/wavebeans/release_notes.html
        * Update links from `/docs/user/api` to `/docs/api`
        * Update links from `/docs/user/http` to `/docs/http`
        * Update links from `/docs/user/cli` to `/docs/cli`
        * Update links from `/docs/user/exe` to `/docs/exe`
        * Update links from `/docs/user/ops` to `/docs/ops`
        * Update links from `/docs/dev` to `/devzone`
        * `readme.md` to `index.html`
        * `.md` to `.html`

6. Social channels:
    * Post the link on twitter and telegram channel.