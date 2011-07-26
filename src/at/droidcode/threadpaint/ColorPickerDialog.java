/*
 * Copyright Maximilian Fellner <max.fellner@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * This file incorporates work covered by the same license and the following copyright:
 * 
 *     Copyright (C) 2007 The Android Open Source Project
 *
 */

package at.droidcode.threadpaint;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.SeekBar;

public class ColorPickerDialog extends AlertDialog implements SeekBar.OnSeekBarChangeListener {
	static final String TAG = "THREADPAINT";

	public interface OnColorChangedListener {
		void colorChanged(int color);
	}

	public interface OnStrokeChangedListener {
		void strokeChanged(int stroke);
	}

	private final OnColorChangedListener colorListener;
	private final OnStrokeChangedListener strokeListener;

	public ColorPickerDialog(Context context, OnColorChangedListener l1, OnStrokeChangedListener l2) {
		super(context);
		colorListener = l1;
		strokeListener = l2;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		OnColorChangedListener l = new OnColorChangedListener() {
			@Override
			public void colorChanged(int color) {
				colorListener.colorChanged(color);
				dismiss();
			}
		};

		setContentView(R.layout.dialog_colorpicker);

		final ColorDialView colorPickerView = (ColorDialView) findViewById(R.id.view_colorpicker);
		colorPickerView.setOnColorChangedListener(l);
		colorPickerView.setInitalColor(PaintView.STDCOLOR);

		final SeekBar strokeSeekBar = (SeekBar) findViewById(R.id.seekbar_stroke);
		strokeSeekBar.setOnSeekBarChangeListener(this);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		final float percent = (float) progress / (float) seekBar.getMax();
		final int strokeWidth = Math.round(PaintView.maxStrokeWidth() * percent);
		final ColorDialView colorPickerView = (ColorDialView) findViewById(R.id.view_colorpicker);
		colorPickerView.setCenterRadius(strokeWidth / 2);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		final float percent = (float) seekBar.getProgress() / (float) seekBar.getMax();
		final int strokeWidth = Math.round(PaintView.maxStrokeWidth() * percent);
		strokeListener.strokeChanged(strokeWidth);
	}
}
