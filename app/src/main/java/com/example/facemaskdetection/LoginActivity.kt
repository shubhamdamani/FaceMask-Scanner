package com.example.facemaskdetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os.uname
import android.util.Log
import com.example.aps1.R
import com.example.facemaskdetection.model.Police

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Police.uname = "Raj";
        Log.e("TAG",Police.uname);
    }
}