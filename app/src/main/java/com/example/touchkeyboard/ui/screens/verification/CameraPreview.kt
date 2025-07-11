package com.example.touchkeyboard.ui.screens.verification


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"
private const val REQUIRED_CONSECUTIVE_SUCCESSES = 3
private const val INFERENCE_INTERVAL_MS = 350L

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    freezeFrame: Bitmap? = null,
    onFrameCaptured: ((Bitmap) -> Unit)? = null,
    analyzing: Boolean = true,
    onHandDetected: (confidence: Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val interpreter = remember { loadModel(context, "model.tflite") }
    var lastBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var lastInferenceTime by remember { mutableStateOf(0L) }
    var consecutiveSuccesses by remember { mutableStateOf(0) }

    // If freezeFrame is provided, show it using Compose Image
    if (freezeFrame != null) {
        Box(modifier = modifier) {
            Image(
                bitmap = freezeFrame.asImageBitmap(),
                contentDescription = "Frozen camera frame",
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        DisposableEffect(analyzing) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(224, 224))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val now = System.currentTimeMillis()
                val bitmap = imageProxy.toBitmap()
                if (bitmap != null) {
                    lastBitmap = bitmap
                    if (analyzing && now - lastInferenceTime > INFERENCE_INTERVAL_MS) {
                        lastInferenceTime = now
                        val (labelIdx, confidence) = classifyHand(interpreter, bitmap)
                        if (labelIdx == 0 && confidence > 0.8f) {
                            consecutiveSuccesses++
                            if (consecutiveSuccesses >= REQUIRED_CONSECUTIVE_SUCCESSES) {
                                onHandDetected(confidence)
                            }
                        } else {
                            consecutiveSuccesses = 0
                        }
                    }
                }
                // If requested, capture the frame
                if (onFrameCaptured != null && bitmap != null) {
                    onFrameCaptured(bitmap)
                }
                imageProxy.close()
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
            onDispose {
                cameraProvider.unbindAll()
                cameraExecutor.shutdown()
            }
        }
        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    }
}

private fun loadModel(context: Context, assetName: String): Interpreter {
    Log.i(TAG, "Loading TFLite model: $assetName")
    val assetFileDescriptor = context.assets.openFd(assetName)
    val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
    val fileChannel = fileInputStream.channel
    val startOffset = assetFileDescriptor.startOffset
    val declaredLength = assetFileDescriptor.declaredLength
    val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

    val interpreter = Interpreter(modelBuffer)

    // Log model details for debugging
    val inputTensor = interpreter.getInputTensor(0)
    Log.i(TAG, "Model input shape: ${inputTensor.shape().contentToString()}")
    Log.i(TAG, "Model input type: ${inputTensor.dataType()}")
    Log.i(TAG, "Model input quantization: zero=${inputTensor.quantizationParams().zeroPoint}, scale=${inputTensor.quantizationParams().scale}")

    val outputTensor = interpreter.getOutputTensor(0)
    Log.i(TAG, "Model output shape: ${outputTensor.shape().contentToString()}")
    Log.i(TAG, "Model output type: ${outputTensor.dataType()}")
    Log.i(TAG, "Model output quantization: zero=${outputTensor.quantizationParams().zeroPoint}, scale=${outputTensor.quantizationParams().scale}")

    Log.i(TAG, "Model loaded successfully")
    return interpreter
}

private fun ImageProxy.toBitmap(): Bitmap? {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, width, height, null)
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 90, out)
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

private fun classifyHand(interpreter: Interpreter, bitmap: Bitmap): Pair<Int, Float> {
    // Get model input details
    val inputShape = interpreter.getInputTensor(0).shape()
    val inputType = interpreter.getInputTensor(0).dataType()

    // Copy bitmap to correct format
    val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)

    // Create and load tensor image with the right data type
    val tensorImage = TensorImage(inputType)
    tensorImage.load(argbBitmap)

    // Process image to correct size (224x224)
    val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
        .build()
    val processedImage = imageProcessor.process(tensorImage)

    // Create output buffer of correct type
    val outputShape = interpreter.getOutputTensor(0).shape()
    val outputType = interpreter.getOutputTensor(0).dataType()
    val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputType)

    // Run inference
    interpreter.run(processedImage.buffer, outputBuffer.buffer.rewind())

    // Process results based on type
    val probabilities: List<Float>
    if (outputType == org.tensorflow.lite.DataType.UINT8) {
        val outputArray = outputBuffer.intArray
        val quantParams = interpreter.getOutputTensor(0).quantizationParams()
        probabilities = outputArray.map { (it - quantParams.zeroPoint) * quantParams.scale }
        Log.d(TAG, "Raw quantized output: ${outputArray.contentToString()}")
    } else {
        val outputArray = outputBuffer.floatArray
        probabilities = outputArray.toList()
        Log.d(TAG, "Raw float output: ${outputArray.contentToString()}")
    }

    Log.d(TAG, "Probabilities: $probabilities")

    val maxIdx = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
    val confidence = if (maxIdx != -1) probabilities[maxIdx].toFloat() else 0f
    return Pair(maxIdx, confidence)
}

private fun softmax(logits: FloatArray): FloatArray {
    val maxLogit = logits.maxOrNull() ?: 0f
    val exps = logits.map { Math.exp((it - maxLogit).toDouble()) }
    val sumExps = exps.sum()
    return exps.map { (it / sumExps).toFloat() }.toFloatArray()
}