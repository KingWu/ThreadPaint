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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import at.droidcode.threadpaint.api.ToolButtonAnimator;
import at.droidcode.threadpaint.dialog.BrushPickerDialog;
import at.droidcode.threadpaint.dialog.BrushPickerDialog.OnBrushChangedListener;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog.OnPaintChangedListener;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * This Activity houses a single PaintView. It handles dialogs and provides an options menu.
 */
public class ThreadPaintActivity extends Activity implements ToolButtonAnimator, OnSharedPreferenceChangeListener {
	private Activity thisActivity;
	private PaintView paintView;
	private ArrayList<View> toolButtons;
	private ColorPickerDialog colorPickerDialog;
	private BrushPickerDialog brushPickerDialog;

	private SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_threadpaint);
		thisActivity = this;

		paintView = (PaintView) findViewById(R.id.view_paint_view);
		paintView.setToolButtonAnimator(this);

		toolButtons = new ArrayList<View>();
		Collections.addAll(toolButtons, findViewById(R.id.btn_color_picker), findViewById(R.id.btn_brush_cap_picker));

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "PaintView destroyed");
		paintView.terminatePaintThread();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle b) {
		Log.d(TAG, "onSaveInstanceState");
		paintView.saveState(b);
	}

	@Override
	protected void onRestoreInstanceState(Bundle b) {
		Log.d(TAG, "onRestoreInstanceState");
		paintView.restoreState(b);
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
		case R.id.menu_save:
			saveBitmap(paintView.getBitmap());
			return true;
		case R.id.menu_clear:
			paintView.fillWithBackgroundColor();
			return true;
		case R.id.menu_prefs:
			Intent i = new Intent(this, ThreadPaintPreferences.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onToolButtonClicked(View button) {
		switch (button.getId()) {
		case R.id.btn_color_picker:
			showColorPickerDialog();
			break;
		case R.id.btn_brush_cap_picker:
			showBrushPickerDialog();
			break;
		default:
		}
	}

	@Override
	public void fadeOutToolButtons() {
		Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.alpha_out);
		animateViews(toolButtons, fadeOut);
	}

	@Override
	public void fadeInToolButtons() {
		Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.alpha_in);
		animateViews(toolButtons, fadeIn);
	}

	/**
	 * Animate all the views with the speciefied animation.
	 * 
	 * @param views Views to animate
	 * @param a Animation to use
	 */
	private void animateViews(ArrayList<View> views, Animation a) {
		Iterator<View> iterator = views.iterator();
		while (iterator.hasNext()) {
			final View view = iterator.next();
			a.reset();
			view.clearAnimation();
			view.startAnimation(a);
		}
	}

	/**
	 * Instantiates a new ColorPickerDialog if necessary and shows it.
	 */
	private void showColorPickerDialog() {
		if (colorPickerDialog == null) {
			final OnPaintChangedListener l = paintView.getOnPaintChangedListener();
			colorPickerDialog = new ColorPickerDialog(this, l);
			paintView.getObservable().addObserver(colorPickerDialog);
		}
		colorPickerDialog.show();
	}

	/**
	 * Instantiates a new BrushPickerDialog if necessary and shows it.
	 */
	private void showBrushPickerDialog() {
		if (brushPickerDialog == null) {
			final OnBrushChangedListener l = paintView.getOnBrushChangedListener();
			brushPickerDialog = new BrushPickerDialog(this, l);
		}
		brushPickerDialog.show();
	}

	private void saveBitmap(final Bitmap bitmap) {
		final String FILENAME = "threadpaint.png";

		Thread thread = new Thread() {
			@Override
			public void run() {
				String state = Environment.getExternalStorageState();

				if (Environment.MEDIA_MOUNTED.equals(state)) {
					File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), FILENAME);
					try {
						FileOutputStream fos = new FileOutputStream(file);
						bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);

						String[] paths = new String[] { file.getAbsolutePath() };
						MediaScannerConnection.scanFile(thisActivity, paths, null, null);

						thisActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								CharSequence text = getResources().getString(R.string.toast_save_success);
								Toast.makeText(thisActivity, text, Toast.LENGTH_SHORT).show();
							}
						});
					} catch (Exception e) {
						Log.e(TAG, "ERROR writing " + file, e);
					}
				} else {
					Log.w(TAG, "Cannot write to external storage!");
				}
			}
		};
		thread.start();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// this only works on devices with a tall screen like phones
		if (preferences.getBoolean("lockorientation", true)) {
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			int screenOrientation;
			switch (display.getRotation()) {
			case Surface.ROTATION_0:
				screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				break;
			case Surface.ROTATION_90:
				screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				break;
			case Surface.ROTATION_270:
				screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				break;
			default:
				screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
			}
			setRequestedOrientation(screenOrientation);
		} else {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
	}
}
