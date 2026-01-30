package com.example.qceqapp.data.remote
import com.example.qceqapp.data.model.GithubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path


interface GithubApiService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubRelease>
}