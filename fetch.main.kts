#!/usr/bin/env kotlinc -script -J-Xmx2g

import java.io.*
import java.net.URL

/** Constants **/
val llvmSourceUrl = "https://github.com/llvm/llvm-project/archive/refs/tags/llvmorg-16.0.6.zip"
val tmpName = "./tmp/tmp.bin"
val buildCommand = "cmake -DLLVM_ENABLE_PROJECTS=clang -DCMAKE_BUILD_TYPE=Release" +
        " -DLLVM_TARGETS_TO_BUILD=\"X86;ARM\'" +
        " -G \"Unix Makefiles\" ../llvm"
val llvmDirectoryName = "llvm-project-llvmorg-16.0.6"

println("start")

/** Actual script **/
// Download source
downloadFile(llvmSourceUrl, tmpName)
"unzip $tmpName -d ./tmp/llvm".run { println(it) }
"mkdir tmp/llvm/llvm-project-llvmorg-16.0.6/build".run { println(it) }
"cd tmp/llvm/${llvmDirectoryName}/build && $buildCommand".run { println(it) }
"cd tmp/llvm/${llvmDirectoryName}/build && make".run { println(it) }
"cp ./tmp/llvm/${llvmDirectoryName}/build/lib/libclang.dylib  ./tmp/libclang.dylib".run { println(it) }
"cp -R ./tmp/llvm/$llvmDirectoryName/clang/include/clang-c ./tmp/".run { println(it) }
"cd ./tmp && zip -r ./headers.zip ./clang-c".run { println(it) }

/*
// Download darwin(mac) SDK and extract headers
downloadFile(darwinUrl, tmpName)
"hdiutil attach $tmpName".run { println(it) }
"cp -R /Volumes/SDL2/SDL2.framework ./tmp/SDL2.framework".run { println(it) }
"cp ./tmp/SDL2.framework/Versions/A/SDL2 ./tmp/libSDL2.dylib".run { println(it) }
"cp -R ./tmp/SDL2.framework/Versions/A/Headers ./tmp/".run { println(it) }
"mv ./tmp/Headers ./tmp/SDL2".run { println(it) }
"cd ./tmp && zip -r ./headers.zip ./SDL2".run { println(it) }

// cleanup
File(tmpName).delete()
File("./tmp/SDL2").deleteRecursively()
File("./tmp/SDL2.framework").deleteRecursively()

// Download windows SDK and extract headers
downloadFile(windowsUrl, tmpName)
"unzip $tmpName -d ./tmp/SDL2".run { println(it) }
"cp ./tmp/SDL2/SDL2.dll ./tmp/libSDL2.dll".run { println(it) }

// cleanup
File(tmpName).delete()
File("./tmp/SDL2").deleteRecursively()*/

/** Utilities **/
fun downloadFile(fileUrl: String, fileName: String) {
    File(fileName).apply {
        if (parentFile.exists().not()) parentFile.mkdirs()
    }
    BufferedInputStream(URL(fileUrl).openStream()).use { bufferedInputStream ->
        FileOutputStream(fileName).use { fileOutputStream ->
            val dataBuffer = ByteArray(1024)
            var bytesRead: Int
            while (bufferedInputStream.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead)
            }
        }
    }

}

fun String.run(onError: (String) -> Unit) {
    val process = ProcessBuilder()
        .command("bash", "-c", this)
        .start()

    val reader = BufferedReader(InputStreamReader(process.errorStream))
    val errors = mutableListOf<String>()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        errors.add(line ?: "")
    }

    if (errors.isNotEmpty()) {
        onError(errors.joinToString("\n"))
    }
}
