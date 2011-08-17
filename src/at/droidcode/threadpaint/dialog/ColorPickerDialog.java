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
 */

package at.droidcode.threadpaint.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.SeekBar;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.ThreadPaintApp;

/**
 * Custom Dialog that provides a color dial to change the selected color and a SeekBar to change the width of the
 * brush's stroke.
 */
public class ColorPickerDialog extends AlertDialog implements SeekBar.OnSeekBarChangeListener {

	public interface OnPaintChangedListener {
		void colorChanged(int color);

		void strokeChanged(int width);
	}

	private final OnPaintChangedListener paintListener;
	private final int maxStrokeWidth;

	public ColorPickerDialog(Context context, OnPaintChangedListener l) {
		super(context);
		paintListener = l;
		maxStrokeWidth = ((ThreadPaintApp) context.getApplicationContext()).maxStrokeWidth();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		OnPaintChangedListener l = new OnPaintChangedListener() {
			@Override
			public void colorChanged(int color) {
				paintListener.colorChanged(color);
				dismiss();
			}

			@Override
			public void strokeChanged(int width) {
				paintListener.strokeChanged(width);
			}
		};

		setContentView(R.layout.dialog_colorpicker);

		final ColorDialView colorDialView = (ColorDialView) findViewById(R.id.view_colorpicker);
		colorDialView.setOnPaintChangedListener(l);

		final int color = getContext().getResources().getColor(R.color.stroke_standard);
		colorDialView.setInitalColor(color);

		final SeekBar strokeSeekBar = (SeekBar) findViewById(R.id.seekbar_stroke);
		strokeSeekBar.setOnSeekBarChangeListener(this);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		final float percent = (float) progress / (float) seekBar.getMax();
		final int strokeWidth = Math.round(maxStrokeWidth * percent);
		final ColorDialView colorPickerView = (ColorDialView) findViewById(R.id.view_colorpicker);
		colorPickerView.setCenterRadius(strokeWidth / 2);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		final float percent = (float) seekBar.getProgress() / (float) seekBar.getMax();
		final int strokeWidth = Math.round(maxStrokeWidth * percent);
		paintListener.strokeChanged(strokeWidth);
	}
}
