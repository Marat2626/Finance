package com.example.finances

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.finances.utils.AppColors
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
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

    var scannedData by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }
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

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.current.screenBG)
    ) {
        // Верхняя панель
        TopAppBar(
            title = {
                Text(
                    text = "Сканировать QR-код чека",
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
                containerColor = Color.Transparent
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
            // Предпросмотр камеры + сканирование QR
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()

                            // Настройка превью
                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)

                            // Настройка анализа изображений (сканирование QR)
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val scanner = BarcodeScanning.getClient()

                            imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (rawValue != null && barcode.valueType == Barcode.TYPE_TEXT) {
                                                    // Нашли QR-код
                                                    scannedData = rawValue
                                                    isScanning = false
                                                    showDialog = true
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("Camera", "Ошибка привязки камеры", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Рамка для наведения на QR-код
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .background(Color.Transparent, RoundedCornerShape(16.dp))
                    )
                }

                // Текст-подсказка
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 150.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "Наведите камеру на QR-код чека",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Кнопка назад
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text("Отмена", color = Color.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // Диалог с данными из QR-кода
    if (showDialog && scannedData != null) {
        QRCodeDataDialog(
            qrData = scannedData!!,
            onConfirm = { amount, category, description ->
                onTransactionDetected(amount, category, description)
                showDialog = false
                onBack()
            },
            onDismiss = {
                showDialog = false
                scannedData = null
            }
        )
    }
}

@Composable
fun QRCodeDataDialog(
    qrData: String,
    onConfirm: (amount: Double, category: String, description: String) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Автоматический парсинг данных чека из QR-кода
    LaunchedEffect(qrData) {
        parseQRData(qrData) { parsedAmount, parsedCategory, parsedDescription ->
            if (parsedAmount != null) amount = parsedAmount.toString()
            if (parsedCategory != null) category = parsedCategory
            if (parsedDescription != null) description = parsedDescription
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Данные из чека", color = Color.White) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "QR-код: ${qrData.take(50)}...",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Сумма") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    placeholder = { Text("Продукты") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFD4AF37),
                        unfocusedBorderColor = Color.Gray
                    )
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
                Text("Добавить", color = Color(0xFF4CAF50))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = Color(0xFFF44336))
            }
        },
        containerColor = Color(0xFF1E2F4A),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// Парсинг данных из QR-кода (формат чека)
private fun parseQRData(
    qrData: String,
    onResult: (amount: Double?, category: String?, description: String?) -> Unit
) {
    // Пример формата QR-кода чека:
    // t=20240101T120000&s=1234.56&fn=8712000100000000&i=12345&fp=9876543210&n=1

    var amount: Double? = null
    var category: String? = null
    var description: String? = null

    // Парсим сумму (s=1234.56)
    val amountPattern = Regex("""[sS]=(\d+[.,]?\d*)""")
    val amountMatch = amountPattern.find(qrData)
    amount = amountMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()

    // Определяем категорию по типу магазина (если есть в данных)
    category = when {
        qrData.contains("магнит", ignoreCase = true) -> "Магазины продуктов"
        qrData.contains("пятёрочка", ignoreCase = true) -> "Магазины продуктов"
        qrData.contains("аптека", ignoreCase = true) -> "Аптеки"
        qrData.contains("азс", ignoreCase = true) -> "АЗС"
        qrData.contains("кафе", ignoreCase = true) -> "Кафе, рестораны, фастфуд"
        else -> "Другое"
    }

    // Описание - дата + время из чека
    val datePattern = Regex("""[tT]=(\d{4})(\d{2})(\d{2})""")
    val dateMatch = datePattern.find(qrData)
    if (dateMatch != null) {
        val year = dateMatch.groupValues[1]
        val month = dateMatch.groupValues[2]
        val day = dateMatch.groupValues[3]
        description = "Чек от $day.$month.$year"
    } else {
        description = "Покупка по чеку"
    }

    onResult(amount, category, description)
}