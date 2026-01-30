package com.example.qceqapp.data.model
import com.google.gson.annotations.SerializedName


data class GithubRelease(
    @SerializedName("tag_name")
    val tagName: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("body")
    val description: String?,

    @SerializedName("assets")
    val assets: List<Asset>,

    @SerializedName("published_at")
    val publishedAt: String
)

data class Asset(
    @SerializedName("name")
    val name: String,

    @SerializedName("browser_download_url")
    val downloadUrl: String,

    @SerializedName("size")
    val size: Long
)