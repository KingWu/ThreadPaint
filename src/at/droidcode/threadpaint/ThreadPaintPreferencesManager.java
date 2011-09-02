package at.droidcode.threadpaint;

import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Manages shared preferences for a list of activities.
 */
class ThreadPaintPreferencesManager implements OnSharedPreferenceChangeListener {
	private final SharedPreferences preferences;
	private final ArrayList<Activity> activities;

	private static final String LOCKORIENTATION = "LOCKORIENTATION";

	public ThreadPaintPreferencesManager(SharedPreferences p) {
		preferences = p;
		activities = new ArrayList<Activity>();
	}

	/**
	 * Adds an Activity to the manager. Preferences will be enforced on the added Activity.
	 * 
	 * @param activity Activity that wants to use the shared preferences
	 */
	void addActivity(Activity activity) {
		activities.add(activity);
		setScreenRotation(preferences, activity);
	}

	/**
	 * @param activity Activity that won't receive shared preference updates anymore
	 */
	void removeActivity(Activity activity) {
		activities.remove(activity);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Iterator<Activity> iterator = activities.iterator();
		while (iterator.hasNext()) {
			final Activity activity = iterator.next();
			if (key.equals(LOCKORIENTATION)) {
				setScreenRotation(sharedPreferences, activity);
			}
		}
	}

	/**
	 * Enforce the screen rotation preference on a Activity. Probably only works on devices with a rectangular screen.
	 */
	private void setScreenRotation(SharedPreferences sharedPreferences, Activity activity) {
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
