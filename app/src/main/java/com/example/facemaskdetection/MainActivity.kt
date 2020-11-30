package com.example.aps1

import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
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
import com.example.facemaskdetection.Box
import com.example.facemaskdetection.model.Signal
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    private var firebaseStore: FirebaseStorage? = null
    private var storageReference: StorageReference? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraView.setLifecycleOwner(this)
        firebaseStore = FirebaseStorage.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
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
            Log.d("RajGarg","Face Detected YO")
            val label = predict(bitmapCropped)
            var predictionn = ""
            val with = label["WithMask"]?: 0F
            val without = label["WithoutMask"]?: 0F
            if (with > without){
                Log.d("RajGarg","Masked face detected")
                predictionn = "With Mask : " + String.format("%.1f", with*100) + "%"
            } else {
                Log.d("RajGarg","Non masked face detected")
                predictionn = "Without Mask : " + String.format("%.1f", without*100) + "%"
                var randStrings = UUID.randomUUID().toString()
                Log.d("RajGarg",randStrings)
                val ref = storageReference?.child("uploads/" + randStrings)
                try{
                    saveMediaToStorage(bitmap)
                    val wrapper = ContextWrapper(applicationContext)
                    // Initializing a new file
                    // The bellow line return a directory in internal storage
                    var file_path = wrapper.getDir("images", Context.MODE_PRIVATE).toString()
//                val file_path: String =
//                    Environment.getDataDirectory().getAbsolutePath().toString() +
//                            "/FaceMaskImg"
                    Log.d("RajGarg","001 YO")
                    val dir = File(file_path)
                    Log.d("RajGarg","002 YO")
                    if (!dir.exists()){
                        dir.mkdirs()
                        Log.d("RajGarg","003 YO")
                    }
                    val file = File(dir, randStrings + ".png")
                    Log.d("RajGarg","004 YO")
                    val fOut = FileOutputStream(file)
                    Log.d("RajGarg","111  Face Detected YO")
                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut)
                    fOut.flush()
                    fOut.close()
                    val finalFilePath = file_path+randStrings+".png"
                    val fPath = finalFilePath.toUri()
                    val uploadTask = ref?.putFile(fPath!!)
                    Log.d("RajGarg","012 YO")
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
                            finish()
                        } else {
                            // Handle failures
                        }
                    }?.addOnFailureListener{

                    }
                    Log.d("RajGarg","222   Face Detected YO")
                }
                catch(e : Exception){
                    Log.d("RajGarg",e.toString())
                }

            }
            boundingBoxList.add(Box(RectF(left, top, right, bottom), predictionn, with>without))
        }
        return boundingBoxList
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

    fun saveMediaToStorage(bitmap: Bitmap) {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

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

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
          //  Toast.makeText(applicationContext,"Saved to Photos",Toast.LENGTH_SHORT).show();
        }
    }

}
