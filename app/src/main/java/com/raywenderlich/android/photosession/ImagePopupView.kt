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

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import kotlinx.android.synthetic.main.image_popup.view.*

class ImagePopupView : FrameLayout {

  companion object {

    const val FADING_ANIMATION_DURATION = 200L

    const val ALPHA_TRANSPARENT = 0.0f
    private const val ALPHA_OPAQUE = 1.0f

    @LayoutRes
    private const val LAYOUT_RESOURCE = R.layout.image_popup

    fun builder(context: Context): ImagePopupBuilder = ImagePopupBuilder(context)
  }

  private var imageDrawable: Drawable? = null
  private var onBackgroundClickAction: () -> Unit = {}

  constructor(context: Context, builder: ImagePopupBuilder) : super(context, null) {
    imageDrawable = builder.imageDrawable
    onBackgroundClickAction = builder.onBackgroundClickAction

    init()
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs, 0) {
    init()
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    init()
  }

  private fun init() {
    alpha = ALPHA_TRANSPARENT

    inflateLayout(context)
    fillContent()
    setClickListeners()
  }

  private fun inflateLayout(context: Context) = LayoutInflater.from(context).inflate(LAYOUT_RESOURCE, this, true)

  private fun fillContent() {
    imagePopup.setImageDrawable(imageDrawable)
    fadeInView()
  }

  private fun setClickListeners() {
    imagePopupRoot.setOnClickListener { onBackgroundClickAction.invoke() }
  }

  private fun fadeInView() {
    imagePopup.visibility = View.VISIBLE
    animate().alpha(ALPHA_OPAQUE)
        .setDuration(FADING_ANIMATION_DURATION)
        .start()
  }

  class ImagePopupBuilder(private val context: Context) {

    var imageDrawable: Drawable? = null
      private set

    var onBackgroundClickAction: () -> Unit = {}
      private set


    fun imageDrawable(imageDrawable: Drawable) = apply { this.imageDrawable = imageDrawable }

    fun onBackgroundClickAction(onBackgroundClickAction: () -> Unit) = apply { this.onBackgroundClickAction = onBackgroundClickAction }

    fun build(): ImagePopupView = ImagePopupView(context, this)
  }
}