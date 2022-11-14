import com.github.sarxos.webcam.Webcam
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.sql.Time
import java.time.LocalDateTime
import java.util.TimeZone
import kotlin.time.TimeSource

fun main() {
//    println("Hello world")

    runBlocking {
        launch {
            var i = 0
            while (true) {
                i++
                val time = LocalDateTime.now()
                println("Running $i: ${LocalDateTime.now()}")
                val webcamNames = Webcam.webcams
                println(webcamNames)
                println()
                delay(2000)
            }
        }
    }




}