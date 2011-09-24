package at.droidcode.threadpaint;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public final class Utils {
	private Utils() {
	}

	public static int dp2px(Context context, int dp) {
		final Resources res = context.getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
		return Math.round(px);
	}

	public static int sreenWidthPx(Context context, float percent) {
		return Math.round(context.getResources().getDisplayMetrics().widthPixels * percent);
	}

	public static void lockScreenOrientation(boolean lock, Activity activity) {
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		if (lock) {
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

	public static class ToastRunnable implements Runnable {
		private final Context context;
		private final String text;

		public ToastRunnable(Context c, String message) {
			context = c;
			text = message;
		}

		@Override
		public void run() {
			Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
		}
	}
}
