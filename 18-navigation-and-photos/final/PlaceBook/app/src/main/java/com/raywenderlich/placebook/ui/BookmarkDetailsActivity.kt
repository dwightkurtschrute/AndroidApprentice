/*
 * Copyright (c) 2018 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.placebook.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.util.ImageUtils
import com.raywenderlich.placebook.viewmodel.BookmarkDetailsViewModel
import kotlinx.android.synthetic.main.activity_bookmark_details.*
import java.io.File

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

  private lateinit var bookmarkDetailsViewModel:
      BookmarkDetailsViewModel
  private var bookmarkDetailsView:
      BookmarkDetailsViewModel.BookmarkDetailsView? = null
  private var photoFile: File? = null

  override fun onCaptureClick() {

    photoFile = null
    try {

      photoFile = ImageUtils.createUniqueImageFile(this)

      if (photoFile == null) {
        return
      }
    } catch (ex: java.io.IOException) {
      return
    }

    val captureIntent =
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)

    val photoUri = FileProvider.getUriForFile(this,
        "com.raywenderlich.placebook.fileprovider",
        photoFile)

    captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
        photoUri)

    val intentActivities = packageManager.queryIntentActivities(
        captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
    intentActivities.map { it.activityInfo.packageName }
        .forEach { grantUriPermission(it, photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }

    startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
  }

  override fun onPickClick() {
    val pickIntent = Intent(Intent.ACTION_PICK,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
  }

  override fun onCreate(savedInstanceState:
                        android.os.Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_bookmark_details)
    setupToolbar()
    setupViewModel()
    getIntentData()
  }

  override fun onCreateOptionsMenu(menu: android.view.Menu):
      Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.menu_bookmark_details, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_save -> {
        saveChanges()
        return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int,
                                data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (resultCode == android.app.Activity.RESULT_OK) {

      when (requestCode) {

        REQUEST_CAPTURE_IMAGE -> {

          val photoFile = photoFile ?: return

          val uri = FileProvider.getUriForFile(this,
              "com.raywenderlich.placebook.fileprovider",
              photoFile)
          revokeUriPermission(uri,
              Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

          val image = getImageWithPath(photoFile.absolutePath)
          image?.let { updateImage(it) }
        }

        REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
          val imageUri = data.data
          val image = getImageWithAuthority(imageUri)
          image?.let { updateImage(it) }
        }
      }
    }
  }

  private fun getImageWithAuthority(uri: Uri): Bitmap? {
    return ImageUtils.decodeUriStreamToSize(uri,
        resources.getDimensionPixelSize(
            R.dimen.default_image_width),
        resources.getDimensionPixelSize(
            R.dimen.default_image_height),
        this)
  }

  private fun updateImage(image: Bitmap) {
    val bookmarkView = bookmarkDetailsView ?: return
    imageViewPlace.setImageBitmap(image)
    bookmarkView.setImage(this, image)
  }

  private fun getImageWithPath(filePath: String): Bitmap? {
    return ImageUtils.decodeFileToSize(filePath,
        resources.getDimensionPixelSize(
            R.dimen.default_image_width),
        resources.getDimensionPixelSize(
            R.dimen.default_image_height))
  }

  private fun replaceImage() {
    val newFragment = PhotoOptionDialogFragment.newInstance(this)
    newFragment?.show(supportFragmentManager, "photoOptionDialog")
  }

  private fun saveChanges() {
    val name = editTextName.text.toString()
    if (name.isEmpty()) {
      return
    }
    bookmarkDetailsView?.let { bookmarkView ->
      bookmarkView.name = editTextName.text.toString()
      bookmarkView.notes = editTextNotes.text.toString()
      bookmarkView.address = editTextAddress.text.toString()
      bookmarkView.phone = editTextPhone.text.toString()
      bookmarkDetailsViewModel.updateBookmark(bookmarkView)
    }
    finish()
  }

  private fun getIntentData() {

    val bookmarkId = intent.getLongExtra(
        MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)

    bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
        this, Observer<BookmarkDetailsViewModel.BookmarkDetailsView> {

      it?.let {
        bookmarkDetailsView = it
        // Populate fields from bookmark
        populateFields()
        populateImageView()
      }
    })
  }

  private fun setupViewModel() {
    bookmarkDetailsViewModel =
        ViewModelProviders.of(this).get(
            BookmarkDetailsViewModel::class.java)
  }

  private fun setupToolbar() {
    setSupportActionBar(toolbar)
  }

  private fun populateFields() {
    bookmarkDetailsView?.let { bookmarkView ->
      editTextName.setText(bookmarkView.name)
      editTextPhone.setText(bookmarkView.phone)
      editTextNotes.setText(bookmarkView.notes)
      editTextAddress.setText(bookmarkView.address)
    }
  }

  private fun populateImageView() {
    bookmarkDetailsView?.let { bookmarkView ->
      val placeImage = bookmarkView.getImage(this)
      placeImage?.let {
        imageViewPlace.setImageBitmap(placeImage)
      }
    }
    imageViewPlace.setOnClickListener {
      replaceImage()
    }
  }

  companion object {
    private const val REQUEST_CAPTURE_IMAGE = 1
    private const val REQUEST_GALLERY_IMAGE = 2
  }
}