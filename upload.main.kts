#!/usr/bin/env kotlinc -script -J-Xmx2g

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.5.1")

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

val githubUser = "klang-toolkit"
val projectName = "libclang-binary"

val version = System.getenv("GITHUB_REF_NAME")
    ?.takeIf { it.isNotBlank() }
    ?: requestFromInput("Please input tag")
val token = System.getenv("GITHUB_TOKEN")
    ?.takeIf { it.isNotBlank() }
    ?: requestFromInput("Please input token")
val uploadUrl = findUploadUrl()

File("./tmp/").walk()
    .filter { it.isFile }
    .forEach { fileToUpload ->
        try {
            uploadFile(fileToUpload)
        } catch (e: Exception) {
            println("Failed to upload ${fileToUpload.name}")
            e.printStackTrace()
        }
    }

println("Upload complete")

fun uploadFile(fileToUpload: File) {
    val fileName = fileToUpload.name
    println("Uploading $fileName to $uploadUrl")
    val uploadRequest = URL("$uploadUrl?name=$fileName")
        .openConnection() as HttpURLConnection
    uploadRequest.requestMethod = "POST"
    uploadRequest.setRequestProperty("Authorization", "Bearer $token")
    uploadRequest.setRequestProperty("Content-Type", "application/octet-stream")
    uploadRequest.setRequestProperty("Content-Length", fileToUpload.length().toString())
    uploadRequest.doOutput = true
    fileToUpload.inputStream().use { input ->
        uploadRequest.outputStream.use { output ->
            input.copyTo(output)
        }
    }
    uploadRequest.inputStream.bufferedReader().use { it.readText() }
        .let { println(it) }
    uploadRequest.disconnect()
}

fun findUploadUrl(): String {

    val releaseUrl = "https://api.github.com/repos/$githubUser/$projectName/releases/tags/$version"
    val releaseRequest = URL(releaseUrl).openConnection() as HttpURLConnection
    releaseRequest.requestMethod = "GET"
    releaseRequest.setRequestProperty("Authorization", "Bearer $token")
    val releaseResponse = releaseRequest.inputStream.bufferedReader().use { it.readText() }
    val json = Json.parseToJsonElement(releaseResponse).jsonObject
    val uploadUrl = json["upload_url"]!!.jsonPrimitive.content
    releaseRequest.disconnect()
    return uploadUrl.substring(0, uploadUrl.indexOf("{"))
}

fun requestFromInput(message: String): String {
    println(message)
    return readlnOrNull()?.takeIf { it.isNotBlank() } ?: requestFromInput(message)
}
