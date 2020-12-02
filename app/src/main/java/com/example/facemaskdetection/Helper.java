package com.example.facemaskdetection;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.aps1.MainActivity;
import com.example.facemaskdetection.model.Signal;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Helper {
    public static void sendNotification(Context context,String itemID) {
        //title?
        String URL = "https://fcm.googleapis.com/fcm/send";
        DatabaseReference databaseReference;
        databaseReference = FirebaseDatabase.getInstance().getReference();
        //aage kya krna h
        for( String i : Signal.Companion.getNearbySignals()){
            RequestQueue mRequestQue;
            mRequestQue = Volley.newRequestQueue(context);
            JSONObject json = new JSONObject();
            try {
                json.put("to","/topics/"+i);
                JSONObject notificationObj = new JSONObject();
                notificationObj.put("title","One intruder detected from :"+Signal.Companion.getSname());
                // ok cool
                notificationObj.put("body","Click to see details");
                json.put("notification",notificationObj);
                JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL,
                        json,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                NotificationsItem obj = new NotificationsItem(itemID,"Click to see details","One intruder detected from :"+Signal.Companion.getSname(),Signal.Companion.getSid());
                                databaseReference.child("Notifications").child(itemID).setValue(obj);
                                Log.d("MUR", "onResponse: ");
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("MUR", "onError: "+error.networkResponse);
                    }
                }
                ){
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String,String> header = new HashMap<>();
                        header.put("content-type","application/json");
                        header.put("authorization","key=AAAAJrPAi-w:APA91bEYsF9_iclkzUraucMypJdPqVx-O9U1HBbHz56Nt6bHZBPvA8db1RHnyVygi5ODvCoj8pNSHfSW3s9VD_06NBfQHwnEJRAZ_K1JP4le9qiTulWQJPOcbJitQTfAAG3wbOUPCmD5");
                        return header;
                    }
                };
                mRequestQue.add(request);
            }
            catch (JSONException e)

            {
                Log.d("notifE",e.toString());
                e.printStackTrace();
            }

        }
    }
}
