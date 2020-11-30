package com.example.facemaskdetection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os.uname
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import com.example.aps1.R
import com.example.facemaskdetection.model.Police
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Police.uname = "Raj";
        Log.e("TAG",Police.uname)
        val db = Firebase.firestore
        val textView: TextView = findViewById(R.id.loginID) as TextView

        val textView2: TextView = findViewById(R.id.loginPSW) as TextView
        var chk: CheckBox
        chk= findViewById(R.id.loginCB)
        val TAG = "RajGarg";
        val button: Button =findViewById(R.id.loginBTN) as Button
        button.setOnClickListener(View.OnClickListener {
            val str: String = textView.text.toString().trim()
            val str2: String = textView2.text.toString().trim()
            if(chk.isChecked){
                db.collection("signal")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result){

                        }
                    }

            }
            else{
                db.collection("police")
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            Log.d(TAG, "${document.id} => ${document.data}")
                            if(document.data["uname"] == str && document.data["upwd"] == str2){
                                Police.uname = document.data["uname"].toString()
                                Police.signal = document.data["signal"].toString()
                                Police.uid = document.data["uid"].toString()
                                Police.upwd = document.data["upwd"].toString()
                                Toast.makeText(this,"Login Successful",Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, PoliceActivity::class.java).apply {

                                }
                                startActivity(intent)

                                Log.d(TAG,"Login Successful!!!")
                                finish()
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this,"Check your internet connection",Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "Check your internet connection", exception)
                    }
            }
        })
    }
}