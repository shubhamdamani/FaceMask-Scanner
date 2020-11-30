package com.example.facemaskdetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.example.aps1.R
import com.example.facemaskdetection.model.Police

class PoliceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_police)
        val button: Button =findViewById(R.id.updBTN) as Button
        val button1: Button =findViewById(R.id.intruderBTN) as Button

        button.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, UpdateActivity::class.java).apply {

            }
            startActivity(intent)
        })
        button1.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, IntruderActivity::class.java).apply {

            }
            startActivity(intent)
        })



    }
}