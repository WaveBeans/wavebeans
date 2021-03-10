package io.wavebeans.http.rtsp

data class RtspRecordingSession(
    val nettyChannelId: String,
    val sessionId: Long,
    val dataChannel: Int,
    val manageChannel: Int,
    val channelMappings: Map<Int, RtpMapping>,
    val bufferHandler: ((Long, RtpMapping, ByteArray) -> Unit),
    val tearDownHandler: ((Long, RtpMapping) -> Unit),
    private var accessedLastTime: Long = Long.MAX_VALUE
) {
    companion object {
        fun create(
            nettyChannelId: String,
            sessionId: Long,
            dataChannel: Int,
            manageChannel: Int,
            channelMappings: Map<Int, RtpMapping>,
            bufferHandler: (Long, RtpMapping, ByteArray) -> Unit,
            tearDownHandler: ((Long, RtpMapping) -> Unit)
        ): RtspRecordingSession {
            return RtspRecordingSession(
                    nettyChannelId,
                    sessionId,
                    dataChannel,
                    manageChannel,
                    channelMappings,
                    bufferHandler,
                    tearDownHandler
            )
        }
    }

    fun accessed() {
        accessedLastTime = System.currentTimeMillis()
    }

    fun isTooOld(ttl: Long): Boolean = System.currentTimeMillis() - accessedLastTime > ttl
}