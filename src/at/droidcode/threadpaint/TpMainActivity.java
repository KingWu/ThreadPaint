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

import static at.droidcode.threadpaint.TpApplication.TAG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import at.droidcode.threadpaint.TpPreferencesActivity.Preference;
import at.droidcode.threadpaint.api.PreferencesCallback;
import at.droidcode.threadpaint.api.ToolButtonAnimator;
import at.droidcode.threadpaint.dialog.BrushPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * This Activity houses a single PaintView. It handles dialogs and provides an options menu.
 */
public class TpMainActivity extends Activity implements ToolButtonAnimator, PreferencesCallback {
	private Activity thisActivity;
	private PaintView paintView;
	private List<View> toolButtons;
	private ColorPickerDialog colorPickerDialog;
	private BrushPickerDialog brushPickerDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_threadpaint);
		thisActivity = this;

		paintView = (PaintView) findViewById(R.id.view_paint_view);
		paintView.setToolButtonAnimator(this);

		toolButtons = new ArrayList<View>();
		Collections.addAll(toolButtons, findViewById(R.id.btn_color_picker), findViewById(R.id.btn_brush_cap_picker));

		TpPreferencesActivity.addCallbackForPreference(this, Preference.LOCKORIENTATION);
		TpPreferencesActivity.addCallbackForPreference(this, Preference.MOVETHRESHOLD);
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "PaintView destroyed");
		TpPreferencesActivity.removeCallback(this);
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
			saveBitmap(paintView.getBitmap(), "threadpaint.png");
			return true;
		case R.id.menu_clear:
			paintView.fillWithBackgroundColor();
			return true;
		case R.id.menu_prefs:
			Intent i = new Intent(this, TpPreferencesActivity.class);
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
	private void animateViews(List<View> views, Animation a) {
		for (int i = 0; i < views.size(); i++) {
			a.reset();
			views.get(i).clearAnimation();
			views.get(i).startAnimation(a);
		}
	}

	/**
	 * Instantiates a new ColorPickerDialog if necessary and shows it.
	 */
	private void showColorPickerDialog() {
		if (colorPickerDialog == null) {
			colorPickerDialog = new ColorPickerDialog(this, paintView);
		}
		colorPickerDialog.show();
	}

	/**
	 * Instantiates a new BrushPickerDialog if necessary and shows it.
	 */
	private void showBrushPickerDialog() {
		if (brushPickerDialog == null) {
			brushPickerDialog = new BrushPickerDialog(this, paintView);
		}
		brushPickerDialog.show();
	}

	private void saveBitmap(final Bitmap bitmap, final String filename) {
		Utils.SaveBitmapThread thread = new Utils.SaveBitmapThread(bitmap, filename, this);
		thread.start();
	}

	@Override
	public void preferenceChanged(SharedPreferences preferences, String key) {
		if (key.equals(Preference.LOCKORIENTATION.key())) {
			boolean lock = preferences.getBoolean(key, true);
			Log.d(TAG, "lockScreenOrientation " + lock);
			Utils.lockScreenOrientation(lock, this);
		} else if (key.equals(Preference.MOVETHRESHOLD.key())) {
			float f = 1.0f;
			try {
				f = Float.parseFloat(preferences.getString(key, "1.0f"));
			} catch (NumberFormatException e) {
				Log.e(TAG, "ERROR ", e);
				CharSequence text = getResources().getString(R.string.toast_float_parse_error);
				Toast.makeText(thisActivity, text, Toast.LENGTH_SHORT).show();
			}
			Log.d(TAG, "setMoveThreshold " + Float.toString(f));
			paintView.setMoveThreshold(f);
		}
	}
}
