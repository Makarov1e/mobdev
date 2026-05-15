package io.github.mobdev.api.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(val name: String, val pwd: String)

data class Message(
    val id: String,
    val from: String,
    val to: String?,
    val data: MessageData,
    val time: String?
)

data class MessageData(
    @SerializedName("Text") val text: TextContent?,
    @SerializedName("Image") val image: ImageContent?
)

data class TextContent(val text: String)

data class ImageContent(val link: String?)

data class SendMessageRequest(
    val from: String,
    val to: String,
    val data: MessageData
)
