package io.wavebeans.http.rtsp

data class RtspControllerChannelState(
    val announcements: MutableSet<MediaAnnouncement>,
    val rtpMappings: MutableSet<RtpMapping>,
    var accessedLastTime: Long,
    var path: String = "/"
) {

    companion object {
        fun create(): RtspControllerChannelState = RtspControllerChannelState(HashSet(), HashSet(), System.currentTimeMillis())
    }
}