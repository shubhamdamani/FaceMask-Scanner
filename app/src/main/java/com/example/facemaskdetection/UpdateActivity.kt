package com.example.facemaskdetection

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.aps1.R
import com.example.facemaskdetection.model.Police
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class UpdateActivity : AppCompatActivity() {
    var newsignal=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)
        val db = Firebase.firestore
        newsignal=Police.signal;

        val signallist=resources.getStringArray(R.array.signallist)
        val button=findViewById<Button>(R.id.btnupd)
        val spinner=findViewById<Spinner>(R.id.spinnerSIG)
        if (spinner != null) {
            val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, signallist)
            spinner.adapter = adapter

            spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>,
                                            view: View, position: Int, id: Long) {
                    newsignal=signallist[position]
//                    Toast.makeText(this@UpdateActivity,
//                                "Selected signal is:" + signallist[position], Toast.LENGTH_SHORT).show()

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    newsignal=Police.signal
//                    Toast.makeText(this@UpdateActivity,
//                        "Please select a signal" , Toast.LENGTH_SHORT).show()
                    // write code to perform some action
                }
            }
        }
        button.setOnClickListener(View.OnClickListener {
            db.collection("police").document(Police.uid).update("signal",newsignal).addOnSuccessListener {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(Police.signal);
                Police.signal=newsignal
                FirebaseMessaging.getInstance().subscribeToTopic(Police.signal);
                Toast.makeText(this,"signal updated",Toast.LENGTH_SHORT).show()
                finish()
            //
            }.addOnFailureListener{
                Toast.makeText(this,"update failed",Toast.LENGTH_SHORT).show()
            }
        })
    }
}