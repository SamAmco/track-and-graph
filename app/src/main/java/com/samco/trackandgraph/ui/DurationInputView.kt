/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import com.samco.trackandgraph.databinding.DurationInputLayoutBinding
import com.samco.trackandgraph.util.focusAndShowKeyboard

class DurationInputView : FrameLayout {
    val binding = DurationInputLayoutBinding.inflate(LayoutInflater.from(context), this, true)
    var seconds = 0L
        private set
    private var listener: ((Long) -> Unit)? = null
    private var doneListener: (() -> Unit)? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        binding.hoursInput.addTextChangedListener { onDurationTextChanged() }
        binding.minutesInput.addTextChangedListener { onDurationTextChanged() }
        binding.secondsInput.addTextChangedListener { onDurationTextChanged() }
        binding.secondsInput.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_DONE) {
                doneListener?.invoke()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        binding.hoursInput.focusAndShowKeyboard()
    }

    override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        return binding.hoursInput.requestFocus()
    }

    private fun onDurationTextChanged() {
        val hours = binding.hoursInput.text.toString().toLongOrNull() ?: 0L
        val minutes = binding.minutesInput.text.toString().toLongOrNull() ?: 0L
        val seconds = binding.secondsInput.text.toString().toLongOrNull() ?: 0L
        val hoursInSeconds = hours * 3600L
        val minutesInSeconds = minutes * 60
        val newSeconds = hoursInSeconds + minutesInSeconds + seconds
        if (this.seconds != newSeconds) {
            this.seconds = newSeconds
            listener?.invoke(this.seconds)
        }
    }

    fun setTimeInSeconds(totalSeconds: Long) {
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60
        val seconds = totalSeconds % 60

        binding.hoursInput.setText(if (hours > 0) hours.toString() else "")
        binding.minutesInput.setText(if (minutes > 0) minutes.toString() else "")
        binding.secondsInput.setText(if (seconds > 0) seconds.toString() else "")

        binding.hoursInput.setSelection(binding.hoursInput.text.length)
        binding.minutesInput.setSelection(binding.minutesInput.text.length)
        binding.secondsInput.setSelection(binding.secondsInput.text.length)
    }

    fun setDoneListener(doneListener: () -> Unit) {
        this.doneListener = doneListener
    }

    fun setDurationChangedListener(listener: (Long) -> Unit) {
        this.listener = listener
    }
}
