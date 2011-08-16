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

package at.droidcode.threadpaint;

import static at.droidcode.threadpaint.ThreadPaintApp.TAG;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog.OnPaintChangedListener;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * This Activity houses a single PaintView. It handles dialogs and provides an
 * options menu.
 */
public class ThreadPaintActivity extends Activity {
	private PaintView paintView;
	private ColorPickerDialog colorPickerDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_threadpaint);
		paintView = (PaintView) findViewById(R.id.paint_view);
	}

	@Override
	public void onPause() {
		Log.w(TAG, "PaintView paused");
		if (colorPickerDialog != null) {
			colorPickerDialog.dismiss();
			colorPickerDialog = null;
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "PaintView destroyed");
		colorPickerDialog = null;
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_color:
			showColorpicker();
			return true;
		case R.id.menu_clear:
			paintView.fillWithBackgroundColor();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Instantiates a new ColorPickerDialog if necessary and shows it.
	 */
	private void showColorpicker() {
		if (colorPickerDialog == null) {
			final OnPaintChangedListener l = paintView.getOnPaintChangedListener();
			colorPickerDialog = new ColorPickerDialog(this, l);
		}
		colorPickerDialog.show();
	}
}
