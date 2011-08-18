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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import at.droidcode.threadpaint.api.ToolButtonAnimator;
import at.droidcode.threadpaint.dialog.BrushPickerDialog;
import at.droidcode.threadpaint.dialog.BrushPickerDialog.OnBrushChangedListener;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog.OnPaintChangedListener;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * This Activity houses a single PaintView. It handles dialogs and provides an options menu.
 */
public class ThreadPaintActivity extends Activity implements ToolButtonAnimator {
	private PaintView paintView;
	private ArrayList<View> toolButtons;
	private ColorPickerDialog colorPickerDialog;
	private BrushPickerDialog brushPickerDialog;
	private SharedPreferences preferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_threadpaint);

		paintView = (PaintView) findViewById(R.id.view_paint_view);
		paintView.setToolButtonAnimator(this);

		toolButtons = new ArrayList<View>();
		Collections.addAll(toolButtons, findViewById(R.id.btn_color_picker), findViewById(R.id.btn_brush_cap_picker));

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	// @Override
	// public void onStart() {
	// super.onStart();
	//
	// Log.d(TAG, "onStart");
	//
	// if (preferences.getBoolean("lockorientation", true)) {
	// Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
	// int screenOrientation;
	// switch (display.getOrientation()) {
	// case Configuration.ORIENTATION_PORTRAIT:
	// screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	// Log.d(TAG, "Lock orientation PORTRAIT");
	// break;
	// case Configuration.ORIENTATION_LANDSCAPE:
	// screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
	// Log.d(TAG, "Lock orientation LANDSCAPE");
	// break;
	// default:
	// screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
	// Log.d(TAG, "Undefined orientation");
	// }
	// setRequestedOrientation(screenOrientation);
	// } else {
	// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
	// Log.d(TAG, "Set orientation SENSOR");
	// }
	// }

	@Override
	public void onDestroy() {
		Log.w(TAG, "PaintView destroyed");
		colorPickerDialog = null;
		brushPickerDialog = null;
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
			showColorPickerDialog();
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
}
