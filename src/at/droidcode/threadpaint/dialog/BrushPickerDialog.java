package at.droidcode.threadpaint.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint.Cap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.ui.PaintView;

public class BrushPickerDialog extends AlertDialog implements View.OnClickListener {

	public interface OnBrushChangedListener {
		void capChanged(Cap cap);
	}

	private final OnBrushChangedListener capListener;

	public BrushPickerDialog(Context context, PaintView p) {
		super(context);
		capListener = p.getOnBrushChangedListener();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_brushpicker);
		final Button round = (Button) findViewById(R.id.btn_cap_round);
		round.setOnClickListener(this);
		final Button square = (Button) findViewById(R.id.btn_cap_squared);
		square.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_cap_round:
			capListener.capChanged(Cap.ROUND);
			dismiss();
			break;
		case R.id.btn_cap_squared:
			capListener.capChanged(Cap.SQUARE);
			dismiss();
			break;
		default:
			dismiss();
		}
	}
}
