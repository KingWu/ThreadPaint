package at.droidcode.threadpaint.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.Utils;

public class SaveFileDialog extends Dialog implements View.OnClickListener {
	private final Bitmap bitmapToSave;
	private EditText editText;

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
				CharSequence text = getContext().getResources().getString(R.string.toast_filename_error);
				Toast toast = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, 0);
				toast.show();
			} else {
				new Utils.SaveBitmapThread(bitmapToSave, filename, getContext()).start();
				dismiss();
			}
			break;
		case R.id.btn_savefile_cancel:
			cancel();
			break;
		}
	}
}
