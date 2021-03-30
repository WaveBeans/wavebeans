include(":lib", ":cli", ":exe", ":http", ":distr", ":proto")
include(":tests")


include(":filesystems")
include(":filesystems:core")
include(":filesystems:dropbox")
project(":filesystems").children.forEach {
    it.name = "filesystems-${it.name}"
}

include(":metrics")
include(":metrics:core")
include(":metrics:prometheus")
project(":metrics").children.forEach {
    it.name = "metrics-${it.name}"
}