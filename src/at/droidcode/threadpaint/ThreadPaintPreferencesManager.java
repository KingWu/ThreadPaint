package at.droidcode.threadpaint;

import static at.droidcode.threadpaint.ThreadPaintApp.TAG;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class ThreadPaintPreferencesManager implements OnSharedPreferenceChangeListener {
	private final SharedPreferences preferences;
	private final ArrayList<Activity> activities;

	private static final String LOCKORIENTATION = "LOCKORIENTATION";

	public ThreadPaintPreferencesManager(SharedPreferences p) {
		preferences = p;
		activities = new ArrayList<Activity>();
	}

	public void addActivity(Activity activity) {
		Log.d(TAG, "addActivity");
		activities.add(activity);
		setScreenRotation(preferences, activity);
	}

	public void removeActivity(Activity activity) {
		Log.d(TAG, "removeActivity");
		activities.remove(activity);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "onSharedPreferenceChanged " + key);
		Iterator<Activity> iterator = activities.iterator();
		while (iterator.hasNext()) {
			final Activity activity = iterator.next();
			if (key.equals(LOCKORIENTATION)) {
				setScreenRotation(sharedPreferences, activity);
			}
		}
	}

	private void setScreenRotation(SharedPreferences sharedPreferences, Activity activity) {
		Log.d(TAG, "setScreenRotation");
		// This only works on devices with a tall screen like phones!
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		if (sharedPreferences.getBoolean(LOCKORIENTATION, true)) {
			Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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
			}
		}
		activity.setRequestedOrientation(screenOrientation);
	}
}
