package at.droidcode.threadpaint;

import static at.droidcode.threadpaint.TpApplication.TAG;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

public class Utils {
	public static int dp2px(Context context, int dp) {
		final Resources r = context.getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
		return Math.round(px);
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

	public static class SaveBitmapThread extends Thread {
		private final Bitmap bitmap;
		private final String name;
		private final Activity activity;

		public SaveBitmapThread(Bitmap bitmap, String name, Activity activity) {
			this.bitmap = bitmap;
			this.name = name;
			this.activity = activity;
		}

		@Override
		public void run() {
			String state = Environment.getExternalStorageState();

			if (Environment.MEDIA_MOUNTED.equals(state)) {
				File file = new File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name);
				try {
					FileOutputStream fos = new FileOutputStream(file);
					bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);

					String[] paths = new String[] { file.getAbsolutePath() };
					MediaScannerConnection.scanFile(activity, paths, null, null);

					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							CharSequence text = activity.getResources().getString(R.string.toast_save_success);
							Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
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
}
