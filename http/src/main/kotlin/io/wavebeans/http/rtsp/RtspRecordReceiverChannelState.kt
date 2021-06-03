package io.wavebeans.http.rtsp

class RtspRecordReceiverChannelState(
    val session: RtspRecordingSession,
        // 0 - searching for the packet,
        // 1 - waiting for the channel,
        // 2 - waiting for the packet size byte 0,
        // 3 - waiting for the packet size byte 1,
        // 4 - reading the buffer
    var currentState: Int = 0,
    var currentBuffer: ByteArray = ByteArray(0),
    var currentPacketSize: Int = 0,
    var bytesLeftToRead: Int = 0,
    var currentChannel: Int = 0,
) {
    companion object {
        fun create(session: RtspRecordingSession): RtspRecordReceiverChannelState {
            return RtspRecordReceiverChannelState(session)
        }
    }
}