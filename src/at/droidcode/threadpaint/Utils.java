package at.droidcode.threadpaint;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				res.getDisplayMetrics());
		return Math.round(px);
	}

	public static int sreenWidthPx(Context context, float percent) {
		return Math.round(context.getResources().getDisplayMetrics().widthPixels * percent);
	}

	public static void lockScreenOrientation(boolean lock, Activity activity) {
		int screenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
		if (lock) {
			Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
					.getDefaultDisplay();
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

	/**
	 * Decodes image and scales it to reduce memory consumption
	 * 
	 * @param f File referring to the image
	 * @return Decoded and scaled Bitmap
	 */
	public static Bitmap decodeFile(Context c, File f) {
		Bitmap tmpBitmap = null;
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 70;

			// Find the correct scale value. It should be the power of 2.
			int tmpWidth = o.outWidth, tmpHeight = o.outHeight;
			int scale = 1;

			while (tmpWidth / 2 < REQUIRED_SIZE || tmpHeight / 2 < REQUIRED_SIZE) {
				tmpWidth /= 2;
				tmpHeight /= 2;
				scale *= 2;
			}

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			tmpBitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, o2);

			// http://sudarnimalan.blogspot.com/2011/09/android-convert-immutable-bitmap-into.html
			// this is the file going to use temporally to save the bytes.
			File file = new File(c.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "tmp");
			file.getParentFile().mkdirs();

			// Open an RandomAccessFile
			RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

			// get the width and height of the source bitmap.
			int width = tmpBitmap.getWidth();
			int height = tmpBitmap.getHeight();

			// Copy the byte to the file
			// Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
			FileChannel channel = randomAccessFile.getChannel();
			MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, width * height * 4);
			tmpBitmap.copyPixelsToBuffer(map);
			// recycle the source bitmap, this will be no longer used.
			tmpBitmap.recycle();
			// Create a new bitmap to load the bitmap again.
			tmpBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			map.position(0);
			// load it back from temporary
			tmpBitmap.copyPixelsFromBuffer(map);
			// close the temporary file and channel , then delete that also
			channel.close();
			randomAccessFile.close();
		} catch (FileNotFoundException e) {
			Log.e(TpApplication.TAG, "ERROR ", e);
		} catch (IOException e) {
			Log.e(TpApplication.TAG, "ERROR ", e);
		}
		return tmpBitmap;
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
