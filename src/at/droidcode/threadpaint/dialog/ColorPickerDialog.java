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
import android.widget.SeekBar;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.ThreadPaintApp;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * Custom Dialog that provides a color dial to change the selected color and a SeekBar to change the width of the
 * brush's stroke.
 */
public class ColorPickerDialog extends AlertDialog implements SeekBar.OnSeekBarChangeListener {
	public interface OnPaintChangedListener {
		void colorChanged(int color);

		void strokeChanged(int width);
	}

	private ColorDialView colorDialView;
	private final PaintView paintView;
	private final OnPaintChangedListener paintListener;
	private final int maxStrokeWidth;

	public ColorPickerDialog(Context context, PaintView p) {
		super(context);
		paintView = p;
		paintListener = p.getOnPaintChangedListener();
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
		colorDialView = (ColorDialView) findViewById(R.id.view_colordial);

		final Button colorButton = (Button) findViewById(R.id.btn_colorpicker_color);
		colorButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorDialView.setGrayscale(false);
			}
		});
		final Button grayButton = (Button) findViewById(R.id.btn_colorpicker_gray);
		grayButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				colorDialView.setGrayscale(true);
			}
		});

		// final TabHost tabHost = (TabHost) findViewById(R.id.colorpicker_tabhost);
		// tabHost.setup();
		//
		// View view1 = createTabView(tabHost.getContext(), "colors");
		// TabHost.TabSpec spec1 =
		// tabHost.newTabSpec("colors").setIndicator(view1).setContent(R.id.colorpicker_content);
		// tabHost.addTab(spec1);

		colorDialView.setOnPaintChangedListener(l);

		final int color = getContext().getResources().getColor(R.color.stroke_standard);
		colorDialView.setInitalColor(color);

		final SeekBar strokeSeekBar = (SeekBar) findViewById(R.id.seekbar_stroke);
		strokeSeekBar.setOnSeekBarChangeListener(this);
	}

	// private static View createTabView(final Context context, final String text) {
	// View view = LayoutInflater.from(context).inflate(R.layout.tab_dialog, null);
	// TextView tv = (TextView) view.findViewById(R.id.dialog_tabs_text);
	// tv.setText(text);
	// return view;
	// }

	@Override
	public void show() {
		super.show();
		colorDialView.setCenterShape(paintView.getPathPaint().getStrokeCap());
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		final float percent = (float) progress / (float) seekBar.getMax();
		final int strokeWidth = Math.round(maxStrokeWidth * percent);
		final ColorDialView colorPickerView = (ColorDialView) findViewById(R.id.view_colordial);
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
