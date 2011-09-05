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

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import at.droidcode.threadpaint.api.PreferencesCallback;

public class ThreadPaintPreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener,
		PreferencesCallback {
	public enum Preference {
		LOCKORIENTATION("pref_orientation"), MOVETHRESHOLD("pref_movethreshold");
		private final String key;

		Preference(String k) {
			key = k;
		}

		String key() {
			return key;
		}
	}

	private static final Hashtable<String, ArrayList<PreferencesCallback>> activityDirectory = new Hashtable<String, ArrayList<PreferencesCallback>>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);

		addCallbackForPreference(this, Preference.LOCKORIENTATION);
	}

	@Override
	public void onDestroy() {
		removeCallback(this);
		super.onDestroy();
	}

	public static void addCallbackForPreference(PreferencesCallback c, Preference preference) {
		if (activityDirectory.containsKey(preference.key())) {
			activityDirectory.get(preference.key()).add(c);
		} else {
			final ArrayList<PreferencesCallback> list = new ArrayList<PreferencesCallback>(1);
			list.add(c);
			activityDirectory.put(preference.key(), list);
		}
	}

	/**
	 * @param c Callback to remove from the directory of callbacks
	 * @return True if the Callback was removed for at least one preference, false otherwise
	 */
	public static boolean removeCallback(PreferencesCallback c) {
		boolean result = false;
		for (Preference p : Preference.values()) {
			ArrayList<PreferencesCallback> list = activityDirectory.get(p);
			if (list != null) {
				list.remove(c);
			}
		}
		return result;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		ArrayList<PreferencesCallback> callbacks = activityDirectory.get(key);
		if (callbacks != null) {
			for (int i = 0; i < callbacks.size(); i++) {
				callbacks.get(i).preferenceChanged(sharedPreferences, key);
			}
		}
	}

	@Override
	public void preferenceChanged(SharedPreferences preferences, String key) {
		if (key.equals(Preference.LOCKORIENTATION.key())) {
			boolean lock = preferences.getBoolean(key, true);
			Utils.lockScreenOrientation(lock, this);
		}
	}
}
