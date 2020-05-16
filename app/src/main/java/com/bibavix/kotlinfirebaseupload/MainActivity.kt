package com.bibavix.kotlinfirebaseupload

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.IOException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.TextView
import java.util.*
import java.io.File
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity(), View.OnClickListener {

    /**
     * Web service variables
     * */

    private var filePath: Uri? = null
    internal var storage: FirebaseStorage? = null
    internal var storageReference: StorageReference? = null
    internal var mAuth: FirebaseAuth? = null
    internal var user: FirebaseUser? = null
    private val PICK_IMAGE_REQUEST: Int = 1234
    /**
     * Camera variables
     * */
    private  val PERMISSION_CODE = 1000
    private  var image_uri: Uri? = null
    private  val REQUEST_TAKE_PHOTO= 1
    lateinit var photoPath : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Init firebase
        storage = FirebaseStorage.getInstance()
        storageReference = storage!!.reference

        //Init firebase auth
        mAuth = FirebaseAuth.getInstance()
        user = mAuth!!.currentUser
        //SetUp Button
        btnChoose.setOnClickListener(this)
        btnUpload.setOnClickListener(this)

        btnCamera.setOnClickListener {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED
                    || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    //Permission was not enabled
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE)
                }else{
                    //Permission was granted
                    openCamera()
                }
            }else{
                openCamera()
            }
        }


    }


    private fun openCamera(){
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) !=null){
            var photoFile: File? = null
            try {
                photoFile = pictureFile()
            }catch (e: IOException){
                e.printStackTrace()
            }
            if (photoFile!=null){
                val photoUri = FileProvider.getUriForFile(this,
                    "com.bibavix.android.provider",photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    private fun pictureFile(): File?
    {
        val fileName = "My Picture"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(fileName,".jpg",storageDir)
        photoPath = image.absolutePath
        return image
    }



    override fun onClick(v: View) {
        if (v === btnChoose) {
            showFileChooser()
        } else if (v === btnUpload) {
            singInAnonymously()
            Toast.makeText(applicationContext, "Success:  $user", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null  && data.data != null){
            filePath = data.data
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, filePath)
                imageView!!.setImageBitmap(bitmap)
            }catch (e: IOException){
                e.printStackTrace()
            }
        }

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK)
        {
            var file: File? = null
            val path: String = Environment.DIRECTORY_PICTURES
            file = File(path, photoPath)
            try {
                var stream: OutputStream? = null
                stream = FileOutputStream(file)
                stream.flush()
                stream.close()
            }catch (e: IOException){
                e.printStackTrace()
            }

            filePath = Uri.parse(photoPath)
            imageView.rotation = 90f;
            imageView.setImageURI(Uri.parse(photoPath))
        }

    }


    private fun singInAnonymously(){
        mAuth?.signInAnonymously()?.addOnSuccessListener {
            showFileUpload()
        }?.addOnFailureListener {
            Toast.makeText(applicationContext, "Failed Sing Up", Toast.LENGTH_SHORT).show()
        }

    }


    private fun showFileChooser(){
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "SELECT PICTURE"), PICK_IMAGE_REQUEST)
    }


    private fun showFileUpload(){
        Log.d("ShowFileUpload", "$filePath")
        if(filePath != null){
            val filename = UUID.randomUUID().toString()
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
            val message = dialogView.findViewById<TextView>(R.id.progress)
            message.text = ""
            builder.setView(dialogView)
            builder.setCancelable(false)
            val dialog = builder.create()
            dialog.dismiss()

            val imageRef = storageReference!!.child("images/$filename")
            imageRef.putFile(filePath!!)
                .addOnSuccessListener {
                    dialog.dismiss()
                    Toast.makeText(applicationContext, "File Uploaded", Toast.LENGTH_SHORT).show()
                    imageRef.downloadUrl.addOnSuccessListener {
                        Log.d("MainActivity: ", "File location on Firebase: $it")
                        saveImageUploadedToDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                    dialog.dismiss()
                    Toast.makeText(applicationContext, "Failed", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                    Log.d("MainActivity", "progress: $progress")
                    message.text = "Uploading..."
                    dialog.show()
                }
        }
    }


    private fun saveImageUploadedToDatabase(imageUrl: String){
        val currentDateTime: Date = Calendar.getInstance().time
        val uid = FirebaseAuth.getInstance().uid?: ""
        val ref =  FirebaseDatabase.getInstance().getReference("/images/$uid")
        val img = ImageTest(currentDateTime.toString(), imageUrl)
        val key = ref!!.push().key
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
        val message = dialogView.findViewById<TextView>(R.id.progress)
        message.text = "Saving..."
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()


        if (key != null) {
            ref!!.child(key).setValue(img).addOnSuccessListener {
                dialog.dismiss()
                Toast.makeText(applicationContext,"Saved", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, GalleryActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }.addOnFailureListener {
                Toast.makeText(applicationContext, "Oops... an error occurred!", Toast.LENGTH_SHORT).show()
            }
        }

    }



}

class ImageTest(val dateUploaded: String, val uriImage: String){
    constructor(): this("","")
}
