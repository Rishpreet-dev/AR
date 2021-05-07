package com.example.ar

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.mindorks.paracamera.Camera
import kotlinx.android.synthetic.main.activity_image_chooser.*
import java.lang.StringBuilder

class ImageChooser : AppCompatActivity() {

    private lateinit var camera:Camera
    private val PERMISSION_REQUEST_CODE=1
    private val REQUEST_CODE_PICK=2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_chooser)


        camera = Camera.Builder()
            .resetToCorrectOrientation(true)
            .setTakePhotoRequestCode(Camera.REQUEST_TAKE_PHOTO)
            .setDirectory("pics")
            .setName("AR_${System.currentTimeMillis()}")
            .setImageFormat(Camera.IMAGE_JPEG)
            .setCompression(75)
            .build(this)

        btTakePicture.setOnClickListener {
            takePicture(it)
        }
        btGallery.setOnClickListener {
            val galleryIntent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent,REQUEST_CODE_PICK)
        }

    }

    fun takePicture(view: View) {
        if (!hasPermission()) {
            // If do not have permissions then request it
            requestPermissions()
        } else {
            // else all permissions granted, go ahead and take a picture using camera
            try {
                camera.takePicture()
            } catch (e: Exception) {
                // Show a toast for exception
                Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermission():Boolean{

        val cameraPermiss= ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)
        val storage=ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
         return cameraPermiss==PackageManager.PERMISSION_GRANTED &&
                 storage==PackageManager.PERMISSION_GRANTED

    }

    private fun requestPermissions(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

//            .snack(getString(R.string.permission_message), Snackbar.LENGTH_INDEFINITE) {
//                action(getString(R.string.OK))
                    ActivityCompat.requestPermissions(this,
                        arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)

            }
         else {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            return
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        camera.takePicture()
                    } catch (e: Exception) {
                        Toast.makeText(this.applicationContext, getString(R.string.error_taking_picture),
                            Toast.LENGTH_SHORT).show()
                    }
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Camera.REQUEST_TAKE_PHOTO) {
                val bitmap = camera.cameraBitmap

                if (bitmap != null) {
                    ivPic.setImageBitmap(bitmap)
                    detectObjectsOnDevice(bitmap)
                } else {
                    Toast.makeText(this.applicationContext, "picture not taken",
                        Toast.LENGTH_SHORT).show()
                }
            }
            if (requestCode==REQUEST_CODE_PICK){
                val uri= data?.data
                labelImage(uri)
            }

        }
    }

    private fun detectObjectsOnDevice(bitmap: Bitmap){
        try {
            val image= bitmap.let {
                FirebaseVisionImage.fromBitmap(it)


            }
            val labeler=FirebaseVision.getInstance().cloudImageLabeler
            labeler.processImage(image)
                .addOnSuccessListener {
                    if (it.isEmpty()){
                        Toast.makeText(this,"No Objects Detected",Toast.LENGTH_LONG).show()
                    }else{
                        val sb=StringBuilder("Recognised Objects: "+"\n")
                        val size=it.size
                        for(i in it){
                            for (j in 1..size)
                                sb.append(it[j-1].text+"\n")
                        }
                        tvResult.text=sb.toString()
                        ivPic.setImageBitmap(bitmap)
                    }
                }
                .addOnFailureListener{
                    Toast.makeText(this,"Did Not Work ",Toast.LENGTH_LONG).show()
                }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }

    private fun labelImage(uri: Uri?){
        try {
            val image= uri?.let {
                FirebaseVisionImage.fromFilePath(this, it)

            }
            val labeler=FirebaseVision.getInstance().cloudImageLabeler
            if (image != null) {
                labeler.processImage(image)
                    .addOnSuccessListener {
                        if (it.isEmpty()){
                            Toast.makeText(this,"No Objects Detected",Toast.LENGTH_LONG).show()
                        }else{
                            val sb=StringBuilder("Recognised Objects: "+"\n")
                            val size=it.size
                            for(i in it){
                                for (j in 1..size)
                                sb.append(it[j-1].text+"\n")
                            }
                            tvResult.text=sb.toString()
                            ivPic.setImageURI(uri)
                        }
                    }
                    .addOnFailureListener{
                        Toast.makeText(this,"Did Not Work ",Toast.LENGTH_LONG).show()
                    }
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }

    }



}