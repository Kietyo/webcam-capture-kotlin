import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.sql.Time
import java.time.LocalDateTime
import java.util.TimeZone
import kotlin.time.TimeSource

private var numCoroutines = 0

fun main() {
    println("Hello world")
    launchCoroutine()
    launchCoroutine()
    launchCoroutine()
    launchCoroutine()

//    runBlocking {
//
//    }
    while(true) {

    }

//    runBlocking {
//        launch {
//            var i = 0
//            while (true) {
//                i++
//                val time = LocalDateTime.now()
//                println("Running $i: ${LocalDateTime.now()}")
//                val webcamNames = Webcam.webcams
//                println(webcamNames)
//                println()
//                delay(2000)
//            }
//        }
//    }
}

fun launchCoroutine() {
    GlobalScope.launch {
        val id = numCoroutines++
        var i = 0
        while (true) {
            i++
            val time = LocalDateTime.now()
            println("$id: Running $i: ${LocalDateTime.now()}")
            val webcamNames = Webcam.webcams
            println(webcamNames)
            println()
            delay(2000)
        }
    }
}