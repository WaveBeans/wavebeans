rootProject.name = "wavebeans"

include(":lib", ":cli", ":exe", ":http", ":distr", ":proto")
include(":tests")


include(":filesystems")
include(":filesystems-core")
include(":filesystems-dropbox")
include(":metrics")
include(":metrics-core")
include(":metrics-prometheus")
include(":tests")

project(":metrics-core").projectDir = file("metrics/core")
project(":metrics-prometheus").projectDir = file("metrics/prometheus")
project(":filesystems-core").projectDir = file("filesystems/core")
project(":filesystems-dropbox").projectDir = file("filesystems/dropbox")

include(":ffmpeg")