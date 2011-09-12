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
import android.view.View;
import android.widget.Button;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * Custom Dialog that provides a color dial to change the selected color and a SeekBar to change the width of the
 * brush's stroke.
 */
public class ColorPickerDialog extends AlertDialog /* implements SeekBar.OnSeekBarChangeListener */{
	public interface OnPaintChangedListener {
		void colorChanged(int color);
	}

	private ColorDialView colorDialView;
	private final PaintView paintView;
	private final OnPaintChangedListener paintListener;

	public ColorPickerDialog(Context context, PaintView p) {
		super(context);
		paintView = p;
		paintListener = p.getOnPaintChangedListener();
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
		};

		setContentView(R.layout.dialog_colorpicker);
		colorDialView = (ColorDialView) findViewById(R.id.view_colordial);

		final Button colorButton = (Button) findViewById(R.id.btn_colorpicker_color);
		colorButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorDialView.setGrayscale(false);
			}
		});
		final Button greyButton = (Button) findViewById(R.id.btn_colorpicker_grey);
		greyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorDialView.setGrayscale(true);
			}
		});

		colorDialView.setOnPaintChangedListener(l);

		final int color = getContext().getResources().getColor(R.color.stroke_standard);
		colorDialView.setInitalColor(color);
	}

	@Override
	public void show() {
		super.show();
		colorDialView.setCenterShape(paintView.getPathPaint().getStrokeCap());
		colorDialView.setCenterRadius(Math.round(paintView.getPathPaint().getStrokeWidth() / 2));
	}
}
