package xyz.dean.kotlin_core.async.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import xyz.dean.kotlin_core.util.log
import java.io.File
import java.util.concurrent.Executors

val kotlinFileFilter = { file: File -> file.isDirectory || file.name.endsWith(".kt") }

data class FileLines(val file: File, val lines: Int) {
    override fun toString(): String {
        return "${file.name}: $lines"
    }
}

suspend fun main() {
    val result = lineCounter(File("."))
    log(result.toString())
}

suspend fun lineCounter(root: File): HashMap<File, Int> {
    return Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1)
        .asCoroutineDispatcher()
        .use { withContext(it) {
            val fileChannel = walkFile(root)

            val fileLinesChannels = List(5) {
                fileLineCounter(fileChannel)
            }

            resultAggregator(fileLinesChannels)
        } }
}

fun CoroutineScope.walkFile(root: File): ReceiveChannel<File> {
    return produce(capacity = BUFFERED) {
        fileWalker(root)
    }
}

suspend fun SendChannel<File>.fileWalker(file: File) {
    if (file.isDirectory) {
        file.listFiles()?.filter(kotlinFileFilter)?.forEach { fileWalker(it) }
    } else {
        send(file)
    }
}

fun CoroutineScope.fileLineCounter(input: ReceiveChannel<File>): ReceiveChannel<FileLines> {
    return produce(capacity = BUFFERED) {
        for (file in input) {
            file.useLines { send(FileLines(file, it.count())) }
        }
    }
}

suspend fun CoroutineScope.resultAggregator(channels: List<ReceiveChannel<FileLines>>): HashMap<File, Int> {
    val map = HashMap<File, Int>()

    channels.aggregate { filteredChannels ->
        // 多路复用的体现
        // 为所有的channel遍历调用onReceiveOrNull来获取结果
        // 直到所有的channel的事件都被处理完毕
        // 本质就是轮询，接收，处理
        select<FileLines?> {
            filteredChannels.forEach {
                it.onReceiveOrNull {
                    log("received: $it")
                    it
                }
            }
        } ?.let {
            map[it.file] = it.lines
        }
    }
    return map
}

tailrec suspend fun List<ReceiveChannel<FileLines>>.aggregate(
    block: suspend (List<ReceiveChannel<FileLines>>) -> Unit
) {
    block(this)
    filter { !it.isClosedForReceive }
        .takeIf { it.isNotEmpty() }
        ?.aggregate(block)
}