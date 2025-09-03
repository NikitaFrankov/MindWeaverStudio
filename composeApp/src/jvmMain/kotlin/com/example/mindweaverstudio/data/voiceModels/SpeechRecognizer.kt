package com.example.mindweaverstudio.data.voiceModels

import com.sun.jna.Native
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SpeechRecognizer() {
    private val modelPath: String = "/Users/nikitaradionov/IdeaProjects/MindWeaver Studio/composeApp/src/jvmMain/kotlin/com/example/mindweaverstudio/data/voiceModels/vosk-model-small-ru-0.22"

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var line: TargetDataLine? = null
    private var job: Job? = null
    val textChannel = Channel<String>(Channel.UNLIMITED)  // Канал для передачи распознанного текста агенту

    init {
        LibVosk.setLogLevel(LogLevel.WARNINGS)
        Native.setProtected(true)  // Для JNA
        model = Model(modelPath)
        recognizer = Recognizer(model, 16000f)  // 16000 Hz — стандарт для Vosk
    }

    fun startRecognition(scope: CoroutineScope) {
        job = scope.launch(Dispatchers.IO) {
            val format = AudioFormat(16000f, 16, 1, true, false)  // Mono, 16-bit, 16kHz
            val info = DataLine.Info(TargetDataLine::class.java, format)
            line = AudioSystem.getLine(info) as TargetDataLine
            line?.open(format)
            line?.start()

            val buffer = ByteArray(4096)
            while (isActive) {
                val bytesRead = line?.read(buffer, 0, buffer.size) ?: 0
                if (bytesRead > 0 && recognizer?.acceptWaveForm(buffer, bytesRead) == true) {
                    val result = recognizer?.result ?: ""
                    val text = result.substringAfter("\"text\" : \"").substringBeforeLast("\"")  // Парсинг JSON-результата
                    if (text.isNotBlank()) {
                        textChannel.send(text)  // Отправляем текст агенту
                    }
                }
            }
        }
    }

    fun stopRecognition() {
        job?.cancel()
        line?.stop()
        line?.close()
        recognizer?.close()
        model?.close()
    }
}