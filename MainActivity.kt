package com.example.myapplication

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.widget.Toast
import android.graphics.BitmapFactory
import java.io.FileInputStream
import java.io.File
import android.content.ContentValues
import java.io.IOException
import java.io.FileOutputStream

import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class MainActivity : Activity() {
    private lateinit var editText: EditText
    private lateinit var imageView: ImageView
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playButton: Button

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        imageView = findViewById(R.id.imageView)
        playButton = findViewById(R.id.playButton)
        mediaPlayer = MediaPlayer()

        dbHelper = DatabaseHelper(this)
        db = dbHelper.writableDatabase

        playButton.setOnClickListener {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
            } else {
                mediaPlayer.pause()
            }
        }
    }

    fun displayImage(view: View) {
        val userInput = editText.text.toString().trim()

        if (userInput.isEmpty()) {
            return
        }

        // Query the database for the image data
        val imageData = getImageDataFromDatabase(userInput)
        if (imageData != null) {
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            imageView.setImageBitmap(bitmap)
        } else {
            Toast.makeText(this, "Image not found in the database.", Toast.LENGTH_SHORT).show()
        }

        // Query the database for the audio data
        val audioData = getAudioDataFromDatabase(userInput)
        if (audioData != null) {
            try {
                // Create a temporary audio file and write the audio data to it
                val tempAudioFile = File(cacheDir, "temp_audio.mp3")
                val outputStream = FileOutputStream(tempAudioFile)
                outputStream.write(audioData)
                outputStream.close()

                // Prepare and start the MediaPlayer with the temporary audio file
                mediaPlayer.reset()
                mediaPlayer.setDataSource(tempAudioFile.absolutePath)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    mediaPlayer.start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error playing audio.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun getImageDataFromDatabase(imageName: String): ByteArray? {
        val projection = arrayOf(DatabaseHelper.ImageEntry.COLUMN_IMAGE)
        val selection = "${DatabaseHelper.ImageEntry.COLUMN_NAME} = ?"
        val selectionArgs = arrayOf(imageName)

        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        var imageData: ByteArray? = null
        if (cursor.moveToFirst()) {
            val imageIndex = cursor.getColumnIndex(DatabaseHelper.ImageEntry.COLUMN_IMAGE)
            imageData = cursor.getBlob(imageIndex)
        }
        cursor.close()

        return imageData
    }

    private fun getAudioDataFromDatabase(audioName: String): ByteArray? {
        val projection = arrayOf(DatabaseHelper.ImageEntry.COLUMN_AUDIO)
        val selection = "${DatabaseHelper.ImageEntry.COLUMN_NAME} = ?"
        val selectionArgs = arrayOf(audioName)

        val cursor: Cursor = db.query(
            DatabaseHelper.TABLE_NAME,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        var audioData: ByteArray? = null
        if (cursor.moveToFirst()) {
            val audioIndex = cursor.getColumnIndex(DatabaseHelper.ImageEntry.COLUMN_AUDIO)
            audioData = cursor.getBlob(audioIndex)
        }
        cursor.close()

        return audioData
    }
}

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "reminiscence.db"
        const val DATABASE_VERSION = 1
        const val TABLE_NAME = "images"
    }

    object ImageEntry : BaseColumns {
        const val COLUMN_NAME = "name"
        const val COLUMN_IMAGE = "image_data"
        const val COLUMN_AUDIO = "audio_data"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableSQL = """
            CREATE TABLE $TABLE_NAME (
                ${BaseColumns._ID} INTEGER PRIMARY KEY,
                ${ImageEntry.COLUMN_NAME} TEXT,
                ${ImageEntry.COLUMN_IMAGE} BLOB,
                ${ImageEntry.COLUMN_AUDIO} BLOB
            )
        """.trimIndent()
        db.execSQL(createTableSQL)

        // Insert the data into the database
        val imageBytes = readBinaryFile("/Users/aparnamohan/Downloads/goldengate.png")
        val audioBytes =
            readBinaryFile("/Users/aparnamohan/Downloads/RiverFlowsInYou.mp3")

        val values = ContentValues()
        values.put(ImageEntry.COLUMN_NAME, "goldengate")
        values.put(ImageEntry.COLUMN_IMAGE, imageBytes)
        values.put(ImageEntry.COLUMN_AUDIO, audioBytes)
        db.insert(TABLE_NAME, null, values)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Database upgrade code, if needed
    }

    // Function to read binary data from a file
    private fun readBinaryFile(filePath: String): ByteArray {
        val file = File(filePath)
        val inputStream = FileInputStream(file)
        val byteArray = inputStream.readBytes()
        inputStream.close()
        return byteArray
    }
}