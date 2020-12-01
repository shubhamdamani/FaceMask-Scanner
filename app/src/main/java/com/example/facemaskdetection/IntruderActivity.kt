package com.example.facemaskdetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aps1.R
import com.example.facemaskdetection.adapter.CustomAdapter
import com.example.facemaskdetection.model.Police
import com.example.facemaskdetection.model.Signal
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class IntruderActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomAdapter
    private lateinit var imageList : List<String>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intruder)
        recyclerView= findViewById(R.id.recyclerView1)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        val db = Firebase.firestore
        val docRef = db.collection("signal").document(Police.signal)
        val TAG = "RajGarg"
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    var tmp = document.data?.get("imagesReceived").toString()
                    Log.d(TAG,tmp)
                    if(tmp.length<=2){
                        Toast.makeText(applicationContext,"No images in the list",Toast.LENGTH_SHORT).show()
                    }
                    else {
                        var tmp2 = tmp.substring(1, tmp.length - 1)
                        imageList = tmp2.split(",").map { it.trim() }
                        Log.d("RajGarg", imageList.toString())
                        adapter = CustomAdapter(imageList)
                        recyclerView.adapter = adapter
                    }
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
        var button : Button
        button = findViewById(R.id.but)
        button.setOnClickListener(View.OnClickListener {
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        var tmp = document.data?.get("imagesReceived").toString()
                        Log.d(TAG,tmp)
                        if(tmp.length<=2){
                            Toast.makeText(applicationContext,"No images in the list",Toast.LENGTH_SHORT).show()
                        }
                        else {
                            var tmp2 = tmp.substring(1, tmp.length - 1)
                            imageList = tmp2.split(",").map { it.trim() }
                            Log.d("RajGarg", imageList.toString())
                            adapter = CustomAdapter(imageList)
                            recyclerView.adapter = adapter
                        }
                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        })
    }
}