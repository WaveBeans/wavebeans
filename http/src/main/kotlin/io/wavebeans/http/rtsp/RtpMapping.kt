package io.wavebeans.http.rtsp

data class RtpMapping(
        val payloadType: Int,
        val encoding: String,
        val clockRate: Int,
        val encodingParameters: String?
)