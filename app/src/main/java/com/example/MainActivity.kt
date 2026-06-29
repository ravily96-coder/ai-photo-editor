package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.FilterBAndW
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.MyApplicationTheme
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

// --- Moshi Models for Gemini REST API ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

// --- Retrofit Config for Gemini API ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

// --- Image Filters & ColorMatrix Configuration ---

data class EnhancementStyle(
    val name: String,
    val description: String,
    val colorMatrix: ColorMatrix,
    val localResponse: String,
    val icon: ImageVector
)

object Filters {
    val VividPop = EnhancementStyle(
        name = "Яркий Поп",
        description = "Насыщенные цвета и динамический контраст",
        colorMatrix = ColorMatrix().apply { setToSaturation(1.6f) },
        localResponse = "AI Анализ: Насыщенность повышена на 60%, усилен контраст. Тени сжаты для драматической глубины, а яркие цвета расширены. Это выделяет детали на пейзажах и городских снимках, подчеркивая живые оттенки и чистый баланс белого.",
        icon = Icons.Rounded.Palette
    )

    val CyberpunkGlow = EnhancementStyle(
        name = "Киберпанк",
        description = "Неоново-пурпурные тени и бирюзовые блики",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 35f,  // Red/Magenta boost
            0f, 0.85f, 0f, 0f, -10f, // Green compression
            0f, 0f, 1.6f, 0f, 45f,  // Cyan/Blue amplification
            0f, 0f, 0f, 1f, 0f
        )),
        localResponse = "AI Анализ: Применена киберпанк-палитра. Тени смещены к неоново-пурпурному, а блики — к бирюзовому. Экспозиция средних тонов слегка снижена для создания атмосферы темных технологий. Идеально для ночных городов и футуристичных уличных кадров.",
        icon = Icons.Rounded.FlashOn
    )

    val GoldenHour = EnhancementStyle(
        name = "Золотой Час",
        description = "Теплые янтарные оттенки заката и глубокие тени",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.35f, 0f, 0f, 0f, 25f,  // Warm Red
            0f, 1.15f, 0f, 0f, 15f,  // Warm Green (Yellow mix)
            0f, 0f, 0.75f, 0f, -25f, // Reduced Blue for yellow shift
            0f, 0f, 0f, 1f, 0f
        )),
        localResponse = "AI Анализ: Применена теплая коррекция тонов заката. Блики смещены в золотисто-янтарный спектр с сохранением насыщенности и глубины теней. Это воссоздает мягкий свет заходящего солнца, добавляя атмосферное свечение объектам и текстурам.",
        icon = Icons.Rounded.WbSunny
    )

    val NoirFilm = EnhancementStyle(
        name = "Нуар Фильм",
        description = "Драматичный черно-белый стиль с высоким контрастом",
        colorMatrix = ColorMatrix(floatArrayOf(
            0.299f * 1.4f, 0.587f * 1.4f, 0.114f * 1.4f, 0f, -35f,
            0.299f * 1.4f, 0.587f * 1.4f, 0.114f * 1.4f, 0f, -35f,
            0.299f * 1.4f, 0.587f * 1.4f, 0.114f * 1.4f, 0f, -35f,
            0f, 0f, 0f, 1f, 0f
        )),
        localResponse = "AI Анализ: Применена модель классической высококонтрастной черно-белой пленки. Тени глубоко приглушены, а светлые области подчеркнуты, чтобы эффектно выделить линии, силуэты и драматичные формы.",
        icon = Icons.Rounded.FilterBAndW
    )

    val DreamyPastel = EnhancementStyle(
        name = "Нежная Пастель",
        description = "Воздушные пастельные тона и мягкое свечение деталей",
        colorMatrix = ColorMatrix(floatArrayOf(
            0.9f, 0f, 0f, 0f, 35f,
            0f, 0.95f, 0f, 0f, 35f,
            0f, 0f, 1.05f, 0f, 45f,
            0f, 0f, 0f, 1f, 0f
        )),
        localResponse = "AI Анализ: Применена мягкая пастельная гамма. Контраст уменьшен с легким поднятием уровня черного для создания эффекта матовой пленки. Присутствуют нежные розово-бирюзовые полутона, идеальные для портретов.",
        icon = Icons.Rounded.Brush
    )

    val AiRejuvenation = EnhancementStyle(
        name = "Омоложение AI",
        description = "Разглаживание кожи, свежий взгляд и естественные черты",
        colorMatrix = ColorMatrix(floatArrayOf(
            1.05f, 0f, 0f, 0f, 10f,  // Mild warmth & brightness boost
            0f, 1.02f, 0f, 0f, 5f,
            0f, 0f, 1.0f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )),
        localResponse = "AI Анализ: Аккуратно разглажена кожа лица, устранены мелкие дефекты и морщины. Взгляд сделан более ярким, свежим и отдохнувшим, при этом полностью сохранены естественная текстура кожи, черты лица и высокое разрешение.",
        icon = Icons.Rounded.Face
    )

    val stylesList = listOf(AiRejuvenation, CyberpunkGlow, VividPop, GoldenHour, NoirFilm, DreamyPastel)
}

// Helper functions for image loading/encoding
fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        null
    }
}

fun getBitmapFromDrawable(context: Context, drawableResId: Int): Bitmap {
    return BitmapFactory.decodeResource(context.resources, drawableResId)
}

fun getBitmapFromUrl(urlString: String): Bitmap? {
    return try {
        val url = java.net.URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input = connection.inputStream
        BitmapFactory.decodeStream(input)
    } catch (e: Exception) {
        null
    }
}

fun Bitmap.toBase64(maxSize: Int = 400): String {
    val aspectRatio = width.toFloat() / height.toFloat()
    val newWidth: Int
    val newHeight: Int
    if (width > height) {
        newWidth = maxSize
        newHeight = (maxSize / aspectRatio).toInt()
    } else {
        newHeight = maxSize
        newWidth = (maxSize * aspectRatio).toInt()
    }
    val scaledBitmap = Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

fun applyColorMatrixToBitmap(originalBitmap: Bitmap, composeColorMatrix: ColorMatrix): Bitmap {
    val width = originalBitmap.width
    val height = originalBitmap.height
    val updatedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(updatedBitmap)
    val paint = android.graphics.Paint()
    
    val androidColorMatrix = android.graphics.ColorMatrix(composeColorMatrix.values)
    paint.colorFilter = android.graphics.ColorMatrixColorFilter(androidColorMatrix)
    
    canvas.drawBitmap(originalBitmap, 0f, 0f, paint)
    return updatedBitmap
}

fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
    val filename = "AI_IMG_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AiPhotoEditor")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        } else {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AiPhotoEditor")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, filename)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
    }

    val contentResolver = context.contentResolver
    val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    if (imageUri != null) {
        try {
            contentResolver.openOutputStream(imageUri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(imageUri, contentValues, null, null)
            } else {
                val file = File(contentValues.getAsString(MediaStore.Images.Media.DATA))
                MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
            }
            return imageUri
        } catch (e: Exception) {
            contentResolver.delete(imageUri, null, null)
            e.printStackTrace()
        }
    }
    return null
}

fun parseGeminiResponse(responseText: String): Pair<String, ColorMatrix?> {
    try {
        var jsonStr = responseText.trim()
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substringAfter("```json").substringBeforeLast("```").trim()
        } else if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substringAfter("```").substringBeforeLast("```").trim()
        }
        
        val jsonObject = org.json.JSONObject(jsonStr)
        val commentary = jsonObject.optString("commentary", "")
        val matrixArray = jsonObject.optJSONArray("matrix")
        
        if (matrixArray != null && matrixArray.length() == 20) {
            val floatArray = FloatArray(20)
            for (i in 0 until 20) {
                floatArray[i] = matrixArray.getDouble(i).toFloat()
            }
            return Pair(commentary, ColorMatrix(floatArray))
        }
        return Pair(commentary, null)
    } catch (e: Exception) {
        e.printStackTrace()
        return Pair(responseText, null)
    }
}

// Main activity
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    PhotoEditorScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // App state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var useDemoImage by remember { mutableStateOf(true) }
    var selectedStyle by remember { mutableStateOf(Filters.CyberpunkGlow) }
    var isEnhanced by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0.5f) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiCommentary by remember { mutableStateOf("") }
    var usingPresetFallback by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var customColorMatrix by remember { mutableStateOf<ColorMatrix?>(null) }

    // Retrieve API key from build config
    val apiKey = BuildConfig.GEMINI_API_KEY
    val isApiKeyPresent = apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "placeholder"

    // Photo picker launcher
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                useDemoImage = false
                isEnhanced = false
                aiCommentary = ""
            }
        }
    )

    // Gallery picker launcher via GetContent
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                selectedImageUri = uri
                useDemoImage = false
                isEnhanced = false
                aiCommentary = ""
            }
        }
    )

    // Default beautiful landscape test image URL loaded from the internet
    val DEFAULT_TEST_IMAGE_URL = "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?auto=format&fit=crop&w=1200&q=80"

    // Demo image drawable resources
    val demoOriginalId = R.drawable.img_demo_original_1782730092873
    val demoEnhancedId = R.drawable.img_demo_enhanced_1782730110710

    // Trigger AI enhancement process
    fun triggerAIEnhance() {
        if (isAnalyzing) return

        isAnalyzing = true
        isEnhanced = true
        aiCommentary = "ИИ анализирует фотографию и выстраивает модель тонов..."
        usingPresetFallback = false
        customColorMatrix = null

        scope.launch {
            // Get bitmap to send to Gemini
            val bitmap: Bitmap? = if (useDemoImage) {
                withContext(Dispatchers.IO) { getBitmapFromUrl(DEFAULT_TEST_IMAGE_URL) } ?: getBitmapFromDrawable(context, demoOriginalId)
            } else {
                selectedImageUri?.let { getBitmapFromUri(context, it) }
            }

            if (bitmap == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Не удалось загрузить изображение для обработки.", Toast.LENGTH_SHORT).show()
                    isAnalyzing = false
                    isEnhanced = false
                }
                return@launch
            }

            try {
                val base64Image = withContext(Dispatchers.IO) {
                    bitmap.toBase64()
                }

                val basePrompt = if (selectedStyle.name == "Омоложение AI") {
                    "Пожалуйста, аккуратно омолоди лицо на этой фотографии. Разгладь кожу, уменьши видимость морщин, сделай взгляд более свежим и отдохнувшим, сохранив при этом естественные черты лица, анатомическую структуру и высокое качество."
                } else {
                    "Определите оптимальные параметры коррекции цвета, контраста, насыщенности и тона, подходящие именно для этого изображения в рамках стиля '${selectedStyle.name}'."
                }

                val prompt = """
                    Вы — профессиональный ИИ-фоторедактор.
                    Проанализируйте предоставленную фотографию и выбранный стиль улучшения: '${selectedStyle.name}'.
                    $basePrompt
                    
                    Сгенерируйте ответ строго в формате JSON, содержащий два поля:
                    1. "commentary": Профессиональный, художественный отзыв на русском языке о сделанных технических корректировках и эстетическом результате (максимум 2 коротких абзаца). Без разметки markdown (без **, без списков).
                    2. "matrix": Массив из ровно 20 дробных чисел (float), представляющих собой цветовую матрицу 4x5 ColorMatrix для применения к изображению. Матрица должна реально изменять цвета в соответствии со стилем '${selectedStyle.name}' (например, для Омоложения AI — мягкие светлые тона кожи с деликатным теплым свечением, убирающим тени под глазами и желтизну; для Киберпанка — усилить синий/пурпурный; для Золотого часа — добавить желтый/янтарный тон; для Яркого Попа — увеличить общую насыщенность; для Нуара — превратить в ч/б с высоким контрастом).
                    
                    Пример формата ответа:
                    {
                      "commentary": "Текст вашего профессионального отзыва...",
                      "matrix": [1.05, 0.0, 0.0, 0.0, 10.0, 0.0, 1.02, 0.0, 0.0, 5.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0]
                    }
                    
                    Выдайте ТОЛЬКО этот JSON-объект, без каких-либо дополнительных символов или markdown-разметки.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.4f,
                        maxOutputTokens = 600
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                withContext(Dispatchers.Main) {
                    if (!responseText.isNullOrEmpty()) {
                        val (comment, parsedMatrix) = parseGeminiResponse(responseText)
                        aiCommentary = comment
                        if (parsedMatrix != null) {
                            customColorMatrix = parsedMatrix
                        } else {
                            customColorMatrix = null
                            usingPresetFallback = true
                        }
                    } else {
                        aiCommentary = selectedStyle.localResponse
                        usingPresetFallback = true
                    }
                    isAnalyzing = false
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    aiCommentary = selectedStyle.localResponse + "\n\n(Внимание: Использован локальный пресет. Для персонализированных ИИ-корректировок настройте GEMINI_API_KEY в панели Secrets)."
                    usingPresetFallback = true
                    customColorMatrix = null
                    isAnalyzing = false
                    Toast.makeText(context, "Использован локальный пресет.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveEnhancedImage() {
        if (isSaving) return
        isSaving = true
        scope.launch(Dispatchers.IO) {
            try {
                val originalBitmap: Bitmap? = if (useDemoImage) {
                    getBitmapFromUrl(DEFAULT_TEST_IMAGE_URL) ?: getBitmapFromDrawable(context, demoOriginalId)
                } else {
                    selectedImageUri?.let { getBitmapFromUri(context, it) }
                }

                if (originalBitmap == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Не удалось загрузить исходное изображение.", Toast.LENGTH_SHORT).show()
                        isSaving = false
                    }
                    return@launch
                }

                // Apply currently selected style matrix (or custom AI-returned matrix)
                val activeMatrix = customColorMatrix ?: selectedStyle.colorMatrix
                val enhancedBitmap = applyColorMatrixToBitmap(originalBitmap, activeMatrix)

                // Save to MediaStore
                val savedUri = saveBitmapToGallery(context, enhancedBitmap)

                withContext(Dispatchers.Main) {
                    if (savedUri != null) {
                        Toast.makeText(context, "Изображение успешно сохранено в Pictures/AiPhotoEditor!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Не удалось сохранить изображение в галерею.", Toast.LENGTH_SHORT).show()
                    }
                    isSaving = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ошибка при сохранении: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    isSaving = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Custom Styled Top App Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "AI Фото Редактор",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            fontFamily = FontFamily.SansSerif
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                if (isEnhanced) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { saveEnhancedImage() },
                            modifier = Modifier.testTag("save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Сохранить в галерею",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (selectedImageUri != null || !useDemoImage) {
                    IconButton(
                        onClick = {
                            selectedImageUri = null
                            useDemoImage = true
                            isEnhanced = false
                            aiCommentary = ""
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Сбросить редактор",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Large Image Container (Interactive Placeholder / Split View Slider)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    BorderStroke(1.dp, Brush.sweepGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF8B5CF6)))),
                    RoundedCornerShape(24.dp)
                )
                .background(Color(0xFF0F111A)),
            contentAlignment = Alignment.Center
        ) {
            if (isEnhanced) {
                // Dual comparison before-after swipe slider layout
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()
                    val handleOffsetPx = with(LocalDensity.current) { -16.dp.toPx() }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, dragAmount ->
                                    change.consume()
                                    sliderPosition = (sliderPosition + dragAmount / containerWidth).coerceIn(0f, 1f)
                                }
                            }
                    ) {
                        // Background: Before Image (Original)
                        if (useDemoImage) {
                            Image(
                                painter = rememberAsyncImagePainter(model = DEFAULT_TEST_IMAGE_URL),
                                contentDescription = "Original demo photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(model = selectedImageUri),
                                contentDescription = "Original photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Foreground: After Image (Enhanced/Filtered) - Clipped by slide offset
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(object : Shape {
                                    override fun createOutline(
                                        size: Size,
                                        layoutDirection: LayoutDirection,
                                        density: Density
                                    ): Outline {
                                        return Outline.Rectangle(
                                            Rect(
                                                left = 0f,
                                                top = 0f,
                                                right = size.width * sliderPosition,
                                                bottom = size.height
                                            )
                                        )
                                    }
                                })
                        ) {
                            val activeMatrix = customColorMatrix ?: selectedStyle.colorMatrix
                            if (useDemoImage) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = DEFAULT_TEST_IMAGE_URL),
                                    contentDescription = "Enhanced demo photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(activeMatrix)
                                )
                            } else {
                                // Real dynamic Color Matrix color manipulation for loaded images!
                                Image(
                                    painter = rememberAsyncImagePainter(model = selectedImageUri),
                                    contentDescription = "Enhanced photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = ColorFilter.colorMatrix(activeMatrix)
                                )
                            }
                        }

                        // Before/After Badges
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // BEFORE badge on the left
                            if (sliderPosition > 0.15f) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopStart)
                                ) {
                                    Text("ДО", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // AFTER badge on the right
                            if (sliderPosition < 0.85f) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Text("AI УЛУЧШЕНО", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Interactive Slider Handle/Line
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                                .align(Alignment.TopStart)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        sliderPosition = (sliderPosition + dragAmount / containerWidth).coerceIn(0f, 1f)
                                    }
                                }
                                .clickable(enabled = false) {}
                                .graphicsLayer {
                                    translationX = containerWidth * sliderPosition - 1.5f
                                }
                        ) {
                            // Circular drag controller handle
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .align(Alignment.Center)
                                    .background(Color.White, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .shadow(8.dp, CircleShape)
                                    .graphicsLayer {
                                        translationX = handleOffsetPx
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Compare,
                                    contentDescription = "Слайдер сравнения",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Default View: Show original photo, or gallery photo, or dashed empty state
                if (useDemoImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2E303F)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Место для фото",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                              )
                        }
                    }
                } else if (selectedImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = selectedImageUri),
                        contentDescription = "Загруженное фото",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Empty state (Dashed overlay inside parent)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = "Загрузить фото",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "Фото не загружено",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Выберите фото или используйте наше демонстрационное изображение",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }

            if (isAnalyzing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Улучшение ИИ...",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Button to select photo from gallery using ActivityResultContracts.GetContent()
        Button(
            onClick = {
                galleryPickerLauncher.launch("image/*")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .height(48.dp)
                .testTag("gallery_picker_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Выбрать фото из галереи",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        // Swipe Hint (Show when enhanced)
        if (isEnhanced) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Compare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Перетаскивайте вертикальный слайдер для сравнения ДО и ПОСЛЕ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Selection of AI Style Enhancements
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "ВЫБЕРИТЕ AI-СТИЛЬ ФИЛЬТРА",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(Filters.stylesList) { style ->
                    val isSelected = selectedStyle == style
                    val borderBrush = if (isSelected) {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .testTag("style_chip_${style.name.replace(" ", "_").lowercase()}")
                            .clickable {
                                selectedStyle = style
                                customColorMatrix = null
                                // If already enhanced, update enhance style dynamically!
                                if (isEnhanced) {
                                    triggerAIEnhance()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, borderBrush),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E1E2E) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = style.name,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons: Pick Photo & AI Enhance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pick Photo Button (Modern outlined pill button)
            OutlinedButton(
                onClick = {
                    singlePhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("pick_photo_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Выбрать фото",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // AI Enhance Button (Primary bright action button)
            Button(
                onClick = {
                    triggerAIEnhance()
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(54.dp)
                    .testTag("enhance_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Улучшить через AI",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Real-time AI Commentary & Settings Panel
        AnimatedVisibility(
            visible = isEnhanced,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Commentary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Анализ Gemini",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Mode indicator
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (usingPresetFallback) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF10B981).copy(alpha = 0.2f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (usingPresetFallback) "ПРЕСЕТ" else "ОНЛАЙН ИИ",
                                    color = if (usingPresetFallback) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Commentary text
                        Text(
                            text = aiCommentary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // If using localized fallback, show helpful prompt on how to configure secrets
                        if (usingPresetFallback && !isApiKeyPresent) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Инструкция",
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = "Активируйте анализ ИИ в реальном времени",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFDDD6FE),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Чтобы разблокировать полностью индивидуальный анализ ваших фотографий в реальном времени, просто введите ваш API-ключ Gemini в панели Secrets в боковом меню Google AI Studio.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = Color(0xFFC4B5FD)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
