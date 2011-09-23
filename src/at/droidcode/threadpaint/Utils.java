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

	public static class SaveBitmapThread extends Thread {
		private final Bitmap bitmap;
		private final String name;
		private final Context context;
		private static final int QUALITY = 90;

		public SaveBitmapThread(Bitmap bitmap, String name, Context activity) {
			this.bitmap = bitmap;
			this.name = name + ".png";
			this.context = activity;
		}

		@Override
		public void run() {
			String state = Environment.getExternalStorageState();

			if (Environment.MEDIA_MOUNTED.equals(state)) {
				File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name);
				try {
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					boolean ok = bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, fileOutputStream);

					String[] paths = new String[] { file.getAbsolutePath() };
					MediaScannerConnection.scanFile(context, paths, null, null);

					TpMainActivity.instance.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							CharSequence text = context.getResources().getString(R.string.toast_save_success);
							Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
						}
					});
					if (ok) {
						Log.w(TAG, file + " successfully saved!");
					}
				} catch (Exception e) {
					Log.e(TAG, "ERROR writing " + file, e);
				}
			} else {
				Log.w(TAG, "Cannot write to external storage!");
			}
			bitmap.recycle();
		}
	};
}
