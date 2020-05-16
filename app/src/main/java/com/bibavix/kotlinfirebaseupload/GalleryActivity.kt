package com.bibavix.kotlinfirebaseupload

import android.content.Intent
import android.media.Image
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.image_row_description.view.*

class GalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        supportActionBar?.title = "Gallery"
      //  val adapter = GroupAdapter<GroupieViewHolder>()

       // recycle_view_gallery.adapter = adapter
        verifyUserIsLoggedIn()
        fetchUsers()
    }

    private fun  fetchUsers(){

       val uid = FirebaseAuth.getInstance().uid
       val ref =  FirebaseDatabase.getInstance().getReference("/images/$uid")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                val adapter = GroupAdapter<GroupieViewHolder>()
                p0.children.forEach{
                    Log.d("Message: ", "${it.toString()}")
                    val image = it.getValue(ImageTest::class.java)
                    if (image != null){
                        adapter.add(ImageItem(image))
                    }
                }
                recycle_view_gallery.adapter = adapter
            }
            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    private fun verifyUserIsLoggedIn(){
        val uid = FirebaseAuth.getInstance().uid
        Log.d("Firebase Auth: ","uid $uid")
        if(uid == null){
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.go_back -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean{
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

}

class ImageItem(private val image: ImageTest): Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_Date.text = image.dateUploaded.toString()
      //
        Picasso.get().load(image.uriImage).into(viewHolder.itemView.imageView_Image)
    }

    override fun getLayout(): Int {
        return R.layout.image_row_description
    }
}
