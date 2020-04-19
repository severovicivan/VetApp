package com.dupre.sandra.dogbreeddetector

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import android.widget.Toast
import kotlinx.android.synthetic.main.prepoznaj_pasminu.*
import java.util.*


class PrepoznajZivotinju : AppCompatActivity(), ZivotinjaView {

    private lateinit var detektorPasmine: DetektorPasmine

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.prepoznaj_pasminu)
        //setSupportActionBar(toolbar)

        detektorPasmine = DetektorPasmine(this)
        detektorPasmine.view = this

        imageView.setOnClickListener {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).let {
                if (it.resolveActivity(packageManager) != null) {
                    startActivityForResult(it, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    override fun onDestroy() {
        detektorPasmine.view = null
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            (data?.extras?.get("data") as Bitmap).apply {
                Bitmap.createBitmap(this, 0, height / 2 - width / 2, width, width).let {
                    imageView.setImageBitmap(it)
                    imageView.scaleType = ImageView.ScaleType.CENTER_CROP
                    detektorPasmine.recognizeDog(bitmap = it)
                }
            }
        }
    }

    override fun displayDogBreed(dogBreed: String, winPercent: Float) {
        textView.text = String.format(Locale.FRANCE, getString(R.string.pas_rezultat), dogBreed,
            winPercent)
    }

    override fun displayError() {
        Toast.makeText(this, R.string.greska, Toast.LENGTH_SHORT).show()
    }

}
