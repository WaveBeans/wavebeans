Distribution project
=======

The purpose of this project is to build a singular distribution of the tools the user may launch locally on his/her machine or servers.

The high level overview
-----

* The project relies on `:exe` and `:cli` projects and copies their artifacts together to deliver as single archive distribution with multiple binaries.
* For certain reasons custom script templates were used, see `resources/unix.txt` and `resources/windows.txt`. See below.
* Distribution allows better tune logging, in the start up scripts the custom logback configuration file is specified, it is done via `-Dlogback.configurationFile=\$APP_HOME/log-config.xml` JVM parameter. That is one of the reasons the custom start up scripts are used.
* To better control the classpath of the java process, added support for `EXTRA_CLASSPATH_BEFORE` and `EXTRA_CLASSPATH_AFTER` environment variables. It allows to add user's classes before or after the required class list. That is another reason the custom start up scripts are used.
