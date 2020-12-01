package com.example.facemaskdetection

import org.json.JSONException

import org.json.JSONObject

//
//private open fun sendNotificatio() {
//    val json = JSONObject()
//    try {
//        json.put("to", "/topics/" + "events")
//        val notificationObj = JSONObject()
//        notificationObj.put("title", itemTitle)
//        notificationObj.put("body", "Click to see details")
//        val extraData = JSONObject()
//        extraData.put("itemId", itemId)
//        extraData.put("category", categ)
//        json.put("notification", notificationObj)
//        json.put("data", extraData)
//        val request: JsonObjectRequest = object : JsonObjectRequest(Request.Method.POST, URL,
//            json,
//            object : Listener<JSONObject?>() {
//                fun onResponse(response: JSONObject?) {
//                    val obj =
//                        NotificationsItem(itemId, "Click to see details", itemTitle, body)
//                    databaseReference.child("Notifications").child(itemId).setValue(obj)
//                    Log.d("MUR", "onResponse: ")
//                }
//            }, object : ErrorListener() {
//                fun onErrorResponse(error: VolleyError) {
//                    Log.d("MUR", "onError: " + error.networkResponse)
//                }
//            }
//        ) {
//            @get:Throws(AuthFailureError::class)
//            val headers: Map<String, String>?
//                get() {
//                    val header: MutableMap<String, String> = HashMap()
//                    header["content-type"] = "application/json"
//                    header["authorization"] =
//                        "key=AAAAJnUq71Q:APA91bFPX6h1jweB072PbEikMvTG300HVuvun0ATUUMMYe6J-RXGp-6Sun0bcTe5jef_Ig9XnFufKFHuWgJjujnkhl25Da9Wf82GQ9JIL39QTf23r15M17PpEPZNsV9-b-ELV9OeoTgE"
//                    return header
//                }
//        }
//        mRequestQue.add(request)
//    } catch (e: JSONException) {
//        Log.d("notifE", e.toString())
//        e.printStackTrace()
//    }
//}