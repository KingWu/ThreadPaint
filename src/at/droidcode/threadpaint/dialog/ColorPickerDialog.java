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
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.TpApplication;
import at.droidcode.threadpaint.dialog.ShapeView.Shape;
import at.droidcode.threadpaint.dialog.ShapeView.ShapeClickedListener;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * Custom Dialog that provides a color dial to change the selected color and a SeekBar to change the width of the
 * brush's stroke.
 */
public class ColorPickerDialog extends AlertDialog implements ShapeClickedListener, SeekBar.OnSeekBarChangeListener {
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

		setContentView(R.layout.dialog_colorpicker);
		colorDialView = (ColorDialView) findViewById(R.id.view_colordial);
		colorDialView.setShapeClickedListener(this);
		int x = ((TpApplication) getContext().getApplicationContext()).maxStrokeWidth();
		colorDialView.setShapeDiameter(x * 0.66f);

		final Button colorButton = (Button) findViewById(R.id.btn_colorpicker_color);
		colorButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorDialView.setGreyscale(false);
			}
		});
		final Button greyButton = (Button) findViewById(R.id.btn_colorpicker_grey);
		greyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorDialView.setGreyscale(true);
			}
		});

		final int color = getContext().getResources().getColor(R.color.stroke_standard);
		colorDialView.setShapeColor(color);

		final SeekBar alphaSeekBar = (SeekBar) findViewById(R.id.seekbar_color_alpha);
		alphaSeekBar.setOnSeekBarChangeListener(this);
	}

	@Override
	public void show() {
		super.show();
		Paint pathPaint = paintView.getPathPaint();
		colorDialView.setShapeColor(pathPaint.getColor());
		if (pathPaint.getStrokeCap() == Cap.SQUARE) {
			colorDialView.setShape(Shape.RECT);
		} else {
			colorDialView.setShape(Shape.CIRCLE);
		}
	}

	@Override
	public void onShapeClicked() {
		Log.d(TpApplication.TAG, "onShapeClicked " + Integer.toHexString(colorDialView.getShapeColor()));
		paintListener.colorChanged(colorDialView.getShapeColor());
		dismiss();
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		colorDialView.setShapeColorAlpha(Math.round(255 * (progress / 100f)));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub

	}
}
