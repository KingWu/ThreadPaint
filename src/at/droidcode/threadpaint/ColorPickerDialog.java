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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.SeekBar;

public class ColorPickerDialog extends Dialog implements SeekBar.OnSeekBarChangeListener {
	static final String TAG = "THREADPAINT";

	public interface OnColorChangedListener {
		void colorChanged(int color);
	}

	public interface OnStrokeChangedListener {
		void strokeChanged(int stroke);
	}

	private final OnColorChangedListener colorListener;
	private final OnStrokeChangedListener strokeListener;
	private final int mInitialColor;
	private int seekBarProgress;

	public ColorPickerDialog(Context context, OnColorChangedListener l1, OnStrokeChangedListener l2, int initialColor) {
		super(context);

		colorListener = l1;
		strokeListener = l2;
		mInitialColor = initialColor;
		seekBarProgress = ColorPickerView.STD_CENTER_RADIUS;
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

		setContentView(R.layout.colorpicker);
		setTitle("Change the Color and Size");

		final ColorPickerView colorPickerView = (ColorPickerView) findViewById(R.id.view_colorpicker);
		colorPickerView.setOnColorChangedListener(l);
		colorPickerView.setInitalColor(mInitialColor);
		final SeekBar strokeSeekBar = (SeekBar) findViewById(R.id.seekbar_stroke);
		strokeSeekBar.setOnSeekBarChangeListener(this);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		seekBarProgress = progress;
		final ColorPickerView colorPickerView = (ColorPickerView) findViewById(R.id.view_colorpicker);
		colorPickerView.setRadiusMultiplier(progress);
		strokeListener.strokeChanged(progress * 2);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		seekBarProgress = seekBar.getProgress();
		final ColorPickerView colorPickerView = (ColorPickerView) findViewById(R.id.view_colorpicker);
		colorPickerView.setRadiusMultiplier(seekBarProgress);
		strokeListener.strokeChanged(seekBarProgress * 2);
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		seekBarProgress = seekBar.getProgress();
		final ColorPickerView colorPickerView = (ColorPickerView) findViewById(R.id.view_colorpicker);
		colorPickerView.setRadiusMultiplier(seekBarProgress);
		strokeListener.strokeChanged(seekBarProgress * 2);
	}
}
