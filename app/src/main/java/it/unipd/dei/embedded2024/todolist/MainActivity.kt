package it.unipd.dei.embedded2024.todolist

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnXMLHome: Button = findViewById(R.id.btnXML)
        btnXMLHome.setOnClickListener { view ->
            val intent = Intent(view.context, XMLHome::class.java)
            view.context.startActivity(intent)
        }

        val btnComposeHome: Button = findViewById(R.id.btnCompose)
        btnComposeHome.setOnClickListener { view ->
            val intent = Intent(view.context, ComposeHome::class.java)
            view.context.startActivity(intent)
        }
    }
}