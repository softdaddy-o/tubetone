package com.tubetone.core.cache

import java.io.File

object CachePolicy {
    data class Result(val evicted: List<File>)

    fun enforce(dir: File, maxBytes: Long, protectedPaths: Set<String>): Result {
        if (!dir.isDirectory) return Result(emptyList())
        val files = dir.walk().filter { it.isFile }.toList().sortedBy { it.lastModified() }
        val total = files.sumOf { it.length() }
        if (total <= maxBytes) return Result(emptyList())
        var running = total
        val evicted = mutableListOf<File>()
        for (f in files) {
            if (running <= maxBytes) break
            if (f.absolutePath in protectedPaths) continue
            running -= f.length()
            if (f.delete()) evicted += f
        }
        return Result(evicted)
    }
}
