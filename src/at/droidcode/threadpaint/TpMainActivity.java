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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;
import at.droidcode.threadpaint.TpPreferencesActivity.Preference;
import at.droidcode.threadpaint.api.PreferencesCallback;
import at.droidcode.threadpaint.api.ToolButtonAnimator;
import at.droidcode.threadpaint.dialog.BrushPickerDialog;
import at.droidcode.threadpaint.dialog.ColorPickerDialog;
import at.droidcode.threadpaint.dialog.SaveFileDialog;
import at.droidcode.threadpaint.ui.PaintView;

/**
 * This Activity houses a single PaintView. It handles dialogs and provides an options menu.
 */
public class TpMainActivity extends Activity implements ToolButtonAnimator, PreferencesCallback {
	public static Activity instance;

	private static final int REQ_LOAD = 1;

	private PaintView paintView;
	private List<View> toolButtons;
	private ColorPickerDialog colorPickerDialog;
	private BrushPickerDialog brushPickerDialog;

	private Button buttonColor;
	private Button buttonBrush;
	private Button buttonMove;
	private Button buttonFill;
	private Button buttonErase;
	private Button buttonUndo;
	private Button buttonRedo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_threadpaint);
		instance = this;

		paintView = (PaintView) findViewById(R.id.view_paint_view);
		paintView.setToolButtonAnimator(this);

		buttonColor = (Button) findViewById(R.id.btn_tool_color);
		buttonBrush = (Button) findViewById(R.id.btn_tool_brush);
		buttonMove = (Button) findViewById(R.id.btn_tool_move);
		buttonFill = (Button) findViewById(R.id.btn_tool_fill);
		buttonErase = (Button) findViewById(R.id.btn_tool_erase);
		buttonUndo = (Button) findViewById(R.id.btn_tool_undo);
		buttonRedo = (Button) findViewById(R.id.btn_tool_redo);

		toolButtons = new ArrayList<View>();
		Collections.addAll(toolButtons, buttonColor, buttonBrush, buttonMove, buttonFill, buttonErase, buttonUndo,
				buttonRedo);

		setSelectedBackground(buttonBrush);

		TpPreferencesActivity.addCallbackForPreference(this, Preference.LOCKORIENTATION);
		TpPreferencesActivity.addCallbackForPreference(this, Preference.MOVETHRESHOLD);
	}

	@Override
	public void onDestroy() {
		Log.w(TAG, "PaintView destroyed");
		TpPreferencesActivity.removeCallback(this);
		paintView.stopPaintThread();
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
			showSaveDialog();
			return true;
		case R.id.menu_load:
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/*");
			startActivityForResult(intent, REQ_LOAD);
			return true;
		case R.id.menu_clear:
			paintView.resetCanvas();
			return true;
		case R.id.menu_prefs:
			Intent i = new Intent(this, TpPreferencesActivity.class);
			startActivity(i);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Tool Button handler declared in xml.
	public void onToolButtonClicked(View button) {
		switch (button.getId()) {
		case R.id.btn_tool_color:
			showColorPickerDialog();
			break;
		case R.id.btn_tool_brush:
			if (paintView.selectedTool() == PaintView.Tool.BRUSH) {
				showBrushPickerDialog();
			} else {
				paintView.selectTool(PaintView.Tool.BRUSH);
				setSelectedBackground(buttonBrush);
			}
			break;
		case R.id.btn_tool_move:
			paintView.selectTool(PaintView.Tool.MOVE);
			setSelectedBackground(buttonMove);
			break;
		case R.id.btn_tool_fill:
			paintView.fillWithPaint();
			break;
		case R.id.btn_tool_erase:
			paintView.selectTool(PaintView.Tool.ERASE);
			paintView.setPaintColor(Color.TRANSPARENT);
			setSelectedBackground(buttonErase);
			break;
		case R.id.btn_tool_undo:
			paintView.undo();
			break;
		case R.id.btn_tool_redo:
			paintView.redo();
			break;
		}
	}

	private void setSelectedBackground(Button button) {
		buttonColor.setBackgroundResource(R.drawable.button_tool_color);
		buttonBrush.setBackgroundResource(R.drawable.button_tool_brush);
		buttonMove.setBackgroundResource(R.drawable.button_tool_move);
		buttonFill.setBackgroundResource(R.drawable.button_tool_fill);
		buttonErase.setBackgroundResource(R.drawable.button_tool_erase);
		switch (button.getId()) {
		case R.id.btn_tool_color:
			button.setBackgroundResource(R.drawable.button_tool_color_selected);
			break;
		case R.id.btn_tool_brush:
			button.setBackgroundResource(R.drawable.button_tool_brush_selected);
			break;
		case R.id.btn_tool_move:
			button.setBackgroundResource(R.drawable.button_tool_move_selected);
			break;
		case R.id.btn_tool_fill:
			button.setBackgroundResource(R.drawable.button_tool_fill_selected);
			break;
		case R.id.btn_tool_erase:
			button.setBackgroundResource(R.drawable.button_tool_erase_selected);
			break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_LOAD:
			if (resultCode == RESULT_OK) {
				Uri imageUri = data.getData();
				String[] filePathColumn = { MediaStore.Images.Media.DATA };

				Cursor cursor = getContentResolver().query(imageUri, filePathColumn, null, null, null);
				cursor.moveToFirst();

				int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
				final File imageFile = new File(cursor.getString(columnIndex));
				cursor.close();

				String loadMessge = getResources().getString(R.string.dialog_load);
				final ProgressDialog load = ProgressDialog.show(TpMainActivity.this, "", loadMessge, true);
				Thread thread = new Thread() {
					@Override
					public void run() {
						Bitmap bitmap = Utils.decodeFile(TpMainActivity.this, imageFile);
						paintView.setBitmap(bitmap);
						load.dismiss();
					}
				};
				thread.start();
			}
			break;
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

	private void showSaveDialog() {
		final SaveFileDialog dialog = new SaveFileDialog(this, paintView.getBitmap());
		dialog.show();
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
				Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
			}
			Log.d(TAG, "setMoveThreshold " + Float.toString(f));
			paintView.setMoveThreshold(f);
		}
	}
}
