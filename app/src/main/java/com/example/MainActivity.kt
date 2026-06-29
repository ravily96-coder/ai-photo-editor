package com.example.aiphotoeditor // Проверь, что это имя пакета совпадает с твоим проектом

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Подключаем кнопки и текст из твоего XML
        val btn = findViewById<Button>(R.id.btn_process_ai)
        val tv = findViewById<TextView>(R.id.tv_ai_result)

        btn.setOnClickListener {
            tv.text = "Обработка..."
            
            MainScope().launch {
                try {
                    // Используем ОФИЦИАЛЬНЫЙ SDK. Никакого Retrofit!
                    val model = GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = "ТВОЙ_КЛЮЧ" // Вставь сюда свой API ключ
                    )
                    
                    val response = model.generateContent("Привет, ты работаешь?")
                    tv.text = response.text
                } catch (e: Exception) {
                    tv.text = "Ошибка: ${e.message}"
                }
            }
        }
    }
}
