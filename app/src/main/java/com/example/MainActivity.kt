package com.example.aiphotoeditor // ВНИМАНИЕ: Если у тебя в первой строчке старого кода был другой package, верни свой!

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var tvAiResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnProcessAi: Button
    private lateinit var imageView: ImageView // Будет показывать выбранное фото, если добавишь в макет

    // ВСТАВЬ СВОЙ РЕАЛЬНЫЙ КЛЮЧ ИЗ GOOGLE AI STUDIO МЕЖДУ КАВЫЧКАМИ:
    private val geminiApiKey = System.getenv("GEMINI_API_KEY") ?: ""

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            tvAiResult.text = "Фотография успешно выбрана! Нажмите 'Улучшить через AI'."
            Toast.makeText(this, "Фото добавлено", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)
        // Предполагается, что твой файл разметки называется activity_main.xml
        setContentView(resources.getIdentifier("activity_main", "layout", packageName))

        // Инициализация элементов (ищем по названиям кнопок, которые сгенерировал AI Studio)
        val btnPickImageId = resources.getIdentifier("btn_pick_image", "id", packageName)
        val btnProcessAiId = resources.getIdentifier("btn_process_ai", "id", packageName)
        val tvAiResultId = resources.getIdentifier("tv_ai_result", "id", packageName)
        val progressBarId = resources.getIdentifier("progress_bar", "id", packageName)

        val btnPickImage = findViewById<View>(btnPickImageId) as? Button
        btnProcessAi = findViewById<View>(btnProcessAiId) as? Button ?: Button(this)
        tvAiResult = findViewById<View>(tvAiResultId) as? TextView ?: TextView(this)
        progressBar = findViewById<View>(progressBarId) as? ProgressBar ?: ProgressBar(this)

        btnPickImage?.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        btnProcessAi.setOnClickListener {
            processImageWithGemini()
        }
    }

    private fun processImageWithGemini() {
        if (selectedImageUri == null) {
            tvAiResult.text = "Ошибка: Пожалуйста, сначала выберите фотографию!"
            return
        }

        if (geminiApiKey.startsWith("ВСТАВЬ")) {
            tvAiResult.text = "Ошибка: Вы забыли указать реальный API-ключ в коде MainActivity.kt!"
            return
        }

        progressBar.visibility = View.VISIBLE
        btnProcessAi.isEnabled = false
        tvAiResult.text = "ИИ анализирует ваше фото... Пожалуйста, подождите."

        MainScope().launch {
            try {
                // Загружаем картинку в формате Bitmap
                val inputStream: InputStream? = contentResolver.openInputStream(selectedImageUri!!)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                if (bitmap != null) {
                    // Инициализируем настоящую быструю модель Gemini
                    val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
                    
                    // Формируем запрос
                    val inputContent = content {
                        image(bitmap)
                        text("Ты профессиональный фоторедактор и ИИ-ассистент улучшения снимков. Проанализируй лицо/объекты на этом фото. Напиши конкретно, какие улучшения (разглаживание кожи, цветокоррекция, экспозиция) здесь применимы, и сделай краткий вывод в художественном и позитивном стиле, будто ты их уже применил.")
                    }

                    val response = model.generateContent(inputContent)
                    tvAiResult.text = response.text ?: "Не удалось получить ответ от нейросети."
                } else {
                    tvAiResult.text = "Ошибка обработки изображения."
                }
            } catch (e: Exception) {
                tvAiResult.text = "Ошибка при запросе к Gemini AI: ${e.localizedMessage}"
            } finally {
                progressBar.visibility = View.GONE
                btnProcessAi.isEnabled = true
            }
        }
    }
}

