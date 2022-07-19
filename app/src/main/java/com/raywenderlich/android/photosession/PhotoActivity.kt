/*
 * Copyright (c) 2019 Razeware LLC
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

package com.raywenderlich.android.photosession

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.Image
import android.os.Bundle
import android.view.Surface
import com.raywenderlich.android.photosession.ImagePopupView.Companion.ALPHA_TRANSPARENT
import com.raywenderlich.android.photosession.ImagePopupView.Companion.FADING_ANIMATION_DURATION
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_photo.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PhotoActivity : AppCompatActivity() {

  companion object {
    private const val REQUEST_CODE_PERMISSIONS = 10

    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
  }

  private val fileUtils: FileUtils by lazy { FileUtilsImpl() }
  private val executor: Executor by lazy { Executors.newSingleThreadExecutor() }

  private var imageCapture: ImageCapture? = null
  private var imagePopupView: ImagePopupView? = null
  private var lensFacing = CameraX.LensFacing.BACK

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_photo)
    setClickListeners()
    requestPermissions()
  }

    private fun startCamera() {
        //Unbind all use cases from the app lifecycle.
        CameraX.unbindAll()
        //Create a preview use case object
        val preview = createPreviewUseCase()
        preview.setOnPreviewOutputUpdateListener {
            // Set a listener that receives the data from the camera. When new data arrives, you release the previous TextureView and add a new one.
            val parent = previewView.parent as ViewGroup
            parent.removeView(previewView)
            parent.addView(previewView, 0)
            previewView.surfaceTexture = it.surfaceTexture

            //call updateTransform which sets the latest transformation to the TextureView
            updateTransform()
        }
        
        imageCapture = createCaptureUseCase()
        //Bind the three use cases to the camera lifecycle. This creates a camera session for them
        CameraX.bindToLifecycle(this, preview, imageCapture)
    }

    private fun createCaptureUseCase(): ImageCapture {
        //You use ImageCaptureConfig.Builder instead of PreviewConfig.Builder.
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetRotation(previewView.display.rotation)
            //You set the capture mode to have the max quality.
            setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
        }
        return ImageCapture(imageCaptureConfig.build())
    }

    private fun updateTransform() {
        val matrix = Matrix()
        // Calculating the center of TextureView.
        val centerX = previewView.width / 2f
        val centerY = previewView.height / 2f
        //Correcting the preview output to account for the rotation of the device.
        val rotationDegrees = when(previewView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)
        //Applying the transformations to TextureView.
        previewView.setTransform(matrix)
    }

    private fun createPreviewUseCase(): Preview {
        //Create a configuration for the preview using the PreviewConfig.Builder helper class provided by CameraX.
        val previewConfig = PreviewConfig.Builder().apply {
            //Set the direction the camera faces using the lensFacing property, which defaults to the rear camera.
            setLensFacing(lensFacing)
            //Set the target rotation for the preview using the orientation from TextureView.
            setTargetRotation(previewView.display.rotation)
        }.build()
        return Preview(previewConfig)
    }

    private fun getMetadata() = ImageCapture.Metadata().apply {
    isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
  }

  private fun setClickListeners() {}

  private fun requestPermissions() {
    if (allPermissionsGranted()) {
        //Run previewView.post to make sure TextureView is ready to use. After this, you start the camera.
        previewView.post { startCamera() }
    } else {
      ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }
  }

  private fun createImagePopup(
      imageDrawable: Drawable,
      backgroundClickAction: () -> Unit
  ) =
      ImagePopupView.builder(this)
          .imageDrawable(imageDrawable)
          .onBackgroundClickAction(backgroundClickAction)
          .build()

  private fun removeImagePopup() {
    imagePopupView?.let {
      it.animate()
          .alpha(ALPHA_TRANSPARENT)
          .setDuration(FADING_ANIMATION_DURATION)
          .withEndAction {
            rootView.removeView(it)
          }
          .start()
    }
  }

  private fun showImagePopup() {
    if (takenImage.drawable == null) {
      return
    }
    createImagePopup(takenImage.drawable) { removeImagePopup() }
        .let {
          imagePopupView = it
          addImagePopupViewToRoot(it)
        }
  }

  private fun addImagePopupViewToRoot(imagePopupView: ImagePopupView) {
    rootView.addView(
        imagePopupView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
  }

  private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
  }

  private fun imageToBitmap(image: Image): Bitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
  }

  private fun disableActions() {
    previewView.isClickable = false
    takenImage.isClickable = false
    toggleCameraLens.isClickable = false
    saveImageSwitch.isClickable = false
  }

  private fun enableActions() {
    previewView.isClickable = true
    takenImage.isClickable = true
    toggleCameraLens.isClickable = true
    saveImageSwitch.isClickable = true
  }

  override fun onRequestPermissionsResult(
      requestCode: Int, permissions: Array<String>, grantResults: IntArray
  ) {
    if (requestCode == REQUEST_CODE_PERMISSIONS) {
      if (allPermissionsGranted()) {
          previewView.post { startCamera() }
      } else {
        finish()
      }
    }
  }

  private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
  }
}
