package at.droidcode.threadpaint.dialog;

import static at.droidcode.threadpaint.TpApplication.TAG;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.TpMainActivity;
import at.droidcode.threadpaint.Utils.ToastRunnable;

public class SaveFileDialog extends Dialog implements View.OnClickListener {
	private final Bitmap bitmapToSave;
	private EditText editText;

	private class SaveBitmapThread extends Thread {
		private final String filename;
		private static final int QUALITY = 90;
		private static final String ENDING = ".png";

		SaveBitmapThread(String name) {
			filename = name + ENDING;
		}

		@Override
		public void run() {
			Context context = getContext();
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
						filename);
				try {
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					bitmapToSave.compress(Bitmap.CompressFormat.PNG, QUALITY, fileOutputStream);

					String[] paths = new String[] { file.getAbsolutePath() };
					MediaScannerConnection.scanFile(context, paths, null, null);

					String success = context.getResources().getString(R.string.toast_save_success);
					TpMainActivity.instance.runOnUiThread(new ToastRunnable(context, success));
				} catch (Exception e) {
					Log.e(TAG, "ERROR writing " + file, e);
				}
			} else {
				String error = context.getResources().getString(R.string.toast_media_not_mounted);
				TpMainActivity.instance.runOnUiThread(new ToastRunnable(context, error));
			}
			bitmapToSave.recycle();
		}
	}

	public SaveFileDialog(Context context, Bitmap bitmap) {
		super(context);
		bitmapToSave = bitmap;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_savefile);
		setTitle(getContext().getResources().getString(R.string.dialog_save));
		findViewById(R.id.btn_savefile_save).setOnClickListener(this);
		findViewById(R.id.btn_savefile_cancel).setOnClickListener(this);
		editText = (EditText) findViewById(R.id.txt_savefile_name);
		editText.setText(getContext().getResources().getString(R.string.default_filename));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_savefile_save:
			String filename = editText.getText().toString();
			if (filename.length() == 0) {
				CharSequence text = getContext().getResources().getString(
						R.string.toast_filename_error);
				Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, 0);
				toast.show();
			} else {
				new SaveBitmapThread(filename).start();
				dismiss();
			}
			break;
		case R.id.btn_savefile_cancel:
			cancel();
			break;
		}
	}
}
