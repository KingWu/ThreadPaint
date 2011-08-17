package at.droidcode.threadpaint.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import at.droidcode.threadpaint.R;

public class BrushPickerDialog extends AlertDialog {

	public BrushPickerDialog(Context context) {
		super(context);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.dialog_brushpicker);
	}

}
