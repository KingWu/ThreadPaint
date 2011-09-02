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

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.Display;
import android.view.WindowManager;

/**
 * This class is used to provide application global variables that need to be initialized on startup.
 */
public class ThreadPaintApp extends Application {
	public static final String TAG = "THREADPAINT";

	private static final int MAX_STROKE_WIDTH_DP_VERT = 150;
	private static final int MAX_STROKE_WIDTH_DP_HORZ = 125;

	private int maxStrokeWidthPx;
	private ThreadPaintPreferencesManager preferencesManager;

	@Override
	public void onCreate() {
		super.onCreate();

		final Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		if (display.getOrientation() == 0) {
			maxStrokeWidthPx = Utils.dp2px(getApplicationContext(), MAX_STROKE_WIDTH_DP_VERT);
		} else {
			maxStrokeWidthPx = Utils.dp2px(getApplicationContext(), MAX_STROKE_WIDTH_DP_HORZ);
		}

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferencesManager = new ThreadPaintPreferencesManager(preferences);
		preferences.registerOnSharedPreferenceChangeListener(preferencesManager);
	}

	/**
	 * @return the maximum width of the brush stroke in pixels, depending on the hardware's pixel density.
	 */
	public int maxStrokeWidth() {
		return maxStrokeWidthPx;
	}

	public ThreadPaintPreferencesManager getPreferencesManager() {
		return preferencesManager;
	}
}
