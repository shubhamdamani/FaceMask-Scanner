package com.example.facemaskdetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.system.Os.uname
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
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
                                Log.d(TAG,"Successful!!!")
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents.", exception)
                    }
            }
        })
    }
}