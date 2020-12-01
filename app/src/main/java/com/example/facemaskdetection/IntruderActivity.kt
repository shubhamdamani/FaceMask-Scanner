package com.example.facemaskdetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.aps1.R
import com.example.facemaskdetection.adapter.CustomAdapter

class IntruderActivity : AppCompatActivity() {
    private lateinit var linearLayoutManager: LinearLayoutManager
    lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CustomAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intruder)
        recyclerView= findViewById(R.id.recyclerView1)
        linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager

        //adapter = CustomAdapter("list")
        recyclerView.adapter = adapter
    }
}