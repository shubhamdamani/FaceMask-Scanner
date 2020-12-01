package com.example.aps1

import android.content.ContentValues
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.android.volley.AuthFailureError
import com.android.volley.Request.Method.POST
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.facemaskdetection.Box
import com.example.facemaskdetection.NotificationsItem
import com.example.facemaskdetection.model.Signal
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import com.android.volley.Request.Method.POST
import com.google.firebase.messaging.FirebaseMessaging
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    private var mRequestQue: RequestQueue? = null
    private val URL = "https://fcm.googleapis.com/fcm/send"
    private var databaseReference: DatabaseReference? = null
    private var firebaseStore: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    private var lastUsed = System.currentTimeMillis()-2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView.setLifecycleOwner(this)
        FirebaseMessaging.getInstance().subscribeToTopic("1");

        databaseReference = FirebaseDatabase.getInstance().getReference();
        firebaseStore = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        mRequestQue = Volley.newRequestQueue(this);
        // Create a FaceDetector
        val faceDetector = FaceDetector.Builder(this).setTrackingEnabled(true)
            .build()
        if (!faceDetector.isOperational) {
            AlertDialog.Builder(this)
                .setMessage("Could not set up the face detector!")
                .show()
        }

        cameraView.addFrameProcessor{ frame ->
            val matrix = Matrix()
            matrix.setRotate(frame.rotationToUser.toFloat())

            if (frame.dataClass === ByteArray::class.java){
                val out = ByteArrayOutputStream()
                val yuvImage = YuvImage(
                    frame.getData(),
                    ImageFormat.NV21,
                    frame.size.width,
                    frame.size.height,
                    null
                )
                yuvImage.compressToJpeg(
                    Rect(0, 0, frame.size.width, frame.size.height), 100, out
                )
                val imageBytes = out.toByteArray()
                var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap = Bitmap.createScaledBitmap(bitmap, overlayView.width, overlayView.height, true)

                overlayView.boundingBox = processBitmap(bitmap, faceDetector)
                overlayView.invalidate()
            } else {
                Toast.makeText(this, "Camera Data not Supported", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun processBitmap(bitmap: Bitmap, faceDetector: FaceDetector): MutableList<Box>{
        val boundingBoxList = mutableListOf<Box>()

        // Detect the faces
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val faces = faceDetector.detect(frame)

        // Mark out the identified face
        for (i in 0 until faces.size()) {
            val thisFace = faces.valueAt(i)
            val left = thisFace.position.x
            val top = thisFace.position.y
            val right = left + thisFace.width
            val bottom = top + thisFace.height
            val bitmapCropped = Bitmap.createBitmap(bitmap,
                left.toInt(),
                top.toInt(),
                if (right.toInt() > bitmap.width) {
                    bitmap.width - left.toInt()
                } else {
                    thisFace.width.toInt()
                },
                if (bottom.toInt() > bitmap.height) {
                    bitmap.height - top.toInt()
                } else {
                    thisFace.height.toInt()
                })
            val label = predict(bitmapCropped)
            var predictionn = ""
            val with = label["WithMask"]?: 0F
            val without = label["WithoutMask"]?: 0F
            if (with > without){
                predictionn = "With Mask : " + String.format("%.1f", with*100) + "%"
            } else {
                predictionn = "Without Mask : " + String.format("%.1f", without*100) + "%"
                if(System.currentTimeMillis()-lastUsed>=2000){
                    lastUsed = System.currentTimeMillis()
                    uploadInCloudStorage(bitmap)
                }
            }
            boundingBoxList.add(Box(RectF(left, top, right, bottom), predictionn, with>without))
        }
        return boundingBoxList
    }

    private fun uploadInCloudStorage(bitmap : Bitmap){
        var randStrings = UUID.randomUUID().toString()
        val uri = saveMediaToStorage(bitmap)
        val ref = storageReference?.child("uploads/" + randStrings)
        val uploadTask = ref?.putFile(uri!!)
        val urlTask = uploadTask?.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        })?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                addUploadRecordToDb(downloadUri.toString())
                sendNotificatio()
                finish()
            } else {
                // Handle failures
            }
        }?.addOnFailureListener{
        }
    }

    private fun addUploadRecordToDb(uri: String){
        val db = Firebase.firestore
        for( i in Signal.nearbySignals) {
            db.collection("signal").document(i)
                .update("imagesReceived", FieldValue.arrayUnion(uri))
        }
    }

    private fun predict(input: Bitmap): MutableMap<String, Float> {
        // load model
        val modelFile = FileUtil.loadMappedFile(this, "model.tflite")
        val model = Interpreter(modelFile, Interpreter.Options()) 
        val labels = FileUtil.loadLabels(this, "labels.txt")

        // data type
        val imageDataType = model.getInputTensor(0).dataType() 
        val inputShape = model.getInputTensor(0).shape() 

        val outputDataType = model.getOutputTensor(0).dataType() 
        val outputShape = model.getOutputTensor(0).shape() 

        var inputImageBuffer = TensorImage(imageDataType)
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType) 

        // preprocess
        val cropSize = kotlin.math.min(input.width, input.height)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize)) 
            .add(ResizeOp(inputShape[1], inputShape[2], ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)) 
            .add(NormalizeOp(127.5f, 127.5f)) 
            .build()

        // load image
        inputImageBuffer.load(input) 
        inputImageBuffer = imageProcessor.process(inputImageBuffer) 

        // run model
        model.run(inputImageBuffer.buffer, outputBuffer.buffer.rewind())

        // get output
        val labelOutput = TensorLabel(labels, outputBuffer) 

        val label = labelOutput.mapWithFloatValue
        return label
    }

    fun saveMediaToStorage(bitmap: Bitmap) : Uri? {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null
        var finalImageUri : Uri? = null
        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            applicationContext?.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    finalImageUri = imageUri
                }
                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            finalImageUri = image.toUri()
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
          //  Toast.makeText(applicationContext,"Saved to Photos",Toast.LENGTH_SHORT).show();
        }
        return finalImageUri
    }

    private fun sendNotificatio() {
        val json = JSONObject()
        try {
            json.put("to", "/topics/" + "events")
            val notificationObj = JSONObject()
            notificationObj.put("title", "1")
            notificationObj.put("body", "Click to see details")
//            val extraData = JSONObject()
//            extraData.put("itemId", itemId)
//            extraData.put("category", categ)
            json.put("notification", notificationObj)
            //json.put("data", extraData)
            val request: JsonObjectRequest =
                object : JsonObjectRequest(
                    POST,
                    URL,
                    json,
                    Response.Listener {
                        val obj =
                            NotificationsItem("1", "Click to see details", "1", "1")
                        databaseReference?.child("Notifications")?.child("1")?.setValue(obj)
                        Log.d("MUR", "onResponse: ")
                    },
                    Response.ErrorListener { error ->
                        Log.d(
                            "MUR",
                            "onError: " + error.networkResponse
                        )
                    }
                ) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        var header= mapOf<String,String>("content-type" to "application/json","authorization" to
                            "key=AAAAJrPAi-w:APA91bEYsF9_iclkzUraucMypJdPqVx-O9U1HBbHz56Nt6bHZBPvA8db1RHnyVygi5ODvCoj8pNSHfSW3s9VD_06NBfQHwnEJRAZ_K1JP4le9qiTulWQJPOcbJitQTfAAG3wbOUPCmD5"
                        )

//                        header["re"]="r"
//                        header.put("content-type", "application/json")
//                        header.put(
//                            "authorization",
//                            "key=AAAAJnUq71Q:APA91bFPX6h1jweB072PbEikMvTG300HVuvun0ATUUMMYe6J-RXGp-6Sun0bcTe5jef_Ig9XnFufKFHuWgJjujnkhl25Da9Wf82GQ9JIL39QTf23r15M17PpEPZNsV9-b-ELV9OeoTgE"
//                        )
                        return header
                    }
                }
            mRequestQue!!.add(request)
        } catch (e: JSONException) {
            Log.d("notifE", e.toString())
            e.printStackTrace()
        }
    }

}
