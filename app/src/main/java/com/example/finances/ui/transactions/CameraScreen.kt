package com.example.finances

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.finances.utils.AppColors
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    onTransactionDetected: (amount: Double, category: String, description: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    // Запрос разрешения на камеру
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // Очистка ресурсов при закрытии экрана
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.screenBlue)
    ) {
        // Верхняя панель
        TopAppBar(
            title = {
                Text(
                    text = "Сканировать чек",
                    color = Color.White,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF2A3A6A)
            )
        )

        if (!hasCameraPermission) {
            // Нет разрешения
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📷",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Нет доступа к камере",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text("Запросить разрешение", color = Color.Black)
                    }
                }
            }
        } else {
            // Предпросмотр камеры
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("Camera", "Ошибка привязки камеры", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Кнопка съемки
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = {
                            if (!isAnalyzing) {
                                takePhotoAndAnalyze(
                                    imageCapture,
                                    cameraExecutor,
                                    onResult = { text ->
                                        recognizedText = text
                                        showDialog = true
                                    },
                                    onStart = { isAnalyzing = true },
                                    onFinish = { isAnalyzing = false }
                                )
                            }
                        },
                        enabled = !isAnalyzing,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Сфотографировать",
                            modifier = Modifier.size(40.dp),
                            tint = Color.Black
                        )
                    }
                }

                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFD4AF37))
                            Text(
                                text = "Анализ чека...",
                                color = Color.White,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Диалог с распознанным текстом
    if (showDialog) {
        RecognizeReceiptDialog(
            recognizedText = recognizedText,
            onConfirm = { amount, category, description ->
                onTransactionDetected(amount, category, description)
                showDialog = false
                onBack()
            },
            onDismiss = { showDialog = false }
        )
    }
}

// Функция для очистки ресурсов
private fun onDispose(block: () -> Unit) {
    block()
}

private fun takePhotoAndAnalyze(
    imageCapture: ImageCapture?,
    cameraExecutor: ExecutorService?,
    onResult: (String) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    if (imageCapture == null || cameraExecutor == null) return

    onStart()

    val outputFile = File.createTempFile("receipt_", ".jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(
        outputOptions,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Анализ изображения через ML Kit
                analyzeImageFile(outputFile, onResult, onFinish)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("Camera", "Ошибка съемки", exception)
                onFinish()
            }
        }
    )
}

private fun analyzeImageFile(
    imageFile: File,
    onResult: (String) -> Unit,
    onFinish: () -> Unit
) {
    try {
        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val image = InputImage.fromBitmap(bitmap, 0)

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val resultText = visionText.text
                onResult(resultText)
                onFinish()
                recognizer.close()
            }
            .addOnFailureListener { e ->
                Log.e("OCR", "Ошибка распознавания", e)
                onResult("")
                onFinish()
                recognizer.close()
            }
    } catch (e: Exception) {
        Log.e("OCR", "Ошибка загрузки изображения", e)
        onResult("")
        onFinish()
    }
}

@Composable
fun RecognizeReceiptDialog(
    recognizedText: String,
    onConfirm: (amount: Double, category: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Автоматический парсинг чека
    LaunchedEffect(recognizedText) {
        parseReceiptText(recognizedText) { parsedAmount, parsedCategory ->
            if (parsedAmount != null) amount = parsedAmount.toString()
            if (parsedCategory != null) category = parsedCategory
        }
        description = recognizedText.take(100)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Распознанный чек", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    placeholder = { Text("Продукты") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: 0.0
                    if (amountValue > 0 && category.isNotEmpty()) {
                        onConfirm(amountValue, category, description)
                    }
                }
            ) {
                Text("Добавить", color = Color(0xFF6BCB6B))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFFFF6B6B))
            }
        },
        containerColor = Color(0xFF1E2F4A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

private fun parseReceiptText(
    text: String,
    onResult: (amount: Double?, category: String?) -> Unit
) {
    // Простой парсинг суммы (ищем числа с двумя знаками после запятой)
    val amountPattern = Regex("""(\d+)[.,](\d{2})\s*[рР]""")
    val match = amountPattern.find(text)
    val amount = match?.let {
        val rub = it.groupValues[1].toDoubleOrNull() ?: 0.0
        val kop = it.groupValues[2].toDoubleOrNull() ?: 0.0
        rub + kop / 100
    }

    // Определение категории по ключевым словам
    val category = when {
        text.contains("продукт", ignoreCase = true) -> "Магазины продуктов"
        text.contains("кафе", ignoreCase = true) -> "Кафе, рестораны, фастфуд"
        text.contains("аптек", ignoreCase = true) -> "Аптеки"
        text.contains("такси", ignoreCase = true) -> "Такси"
        text.contains("супермаркет", ignoreCase = true) -> "Магазины продуктов"
        text.contains("магнит", ignoreCase = true) -> "Магазины продуктов"
        text.contains("пятерочка", ignoreCase = true) -> "Магазины продуктов"
        else -> null
    }

    onResult(amount, category)
}