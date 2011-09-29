package at.droidcode.threadpaint.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint.Cap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import at.droidcode.threadpaint.R;
import at.droidcode.threadpaint.TpApplication;
import at.droidcode.threadpaint.dialog.ShapeView.Shape;
import at.droidcode.threadpaint.dialog.ShapeView.ShapeClickedListener;
import at.droidcode.threadpaint.ui.PaintView;

public class BrushPickerDialog extends AlertDialog implements View.OnClickListener,
		OnSeekBarChangeListener, ShapeClickedListener {
	public interface OnBrushChangedListener {
		void capChanged(Cap cap);

		void strokeChanged(int width);
	}

	private BrushTipView brushTipView;
	private final PaintView paintView;
	private final int maxStrokeWidth;
	private final OnBrushChangedListener brushListener;

	public BrushPickerDialog(Context context, PaintView p) {
		super(context);
		maxStrokeWidth = ((TpApplication) context.getApplicationContext()).maxStrokeWidth();
		brushListener = p.getOnBrushChangedListener();
		paintView = p;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_brushpicker);

		brushTipView = (BrushTipView) findViewById(R.id.view_brushtip);
		brushTipView.setShapeClickedListener(this);
		int x = ((TpApplication) getContext().getApplicationContext()).maxStrokeWidth();
		brushTipView.setShapeDiameter(x / 2f);

		final Button round = (Button) findViewById(R.id.btn_cap_round);
		round.setOnClickListener(this);
		final Button square = (Button) findViewById(R.id.btn_cap_squared);
		square.setOnClickListener(this);

		final SeekBar strokeSeekBar = (SeekBar) findViewById(R.id.seekbar_brush_tip);
		strokeSeekBar.setOnSeekBarChangeListener(this);
	}

	@Override
	public void show() {
		super.show();
		brushTipView.setShapeColor(paintView.getPathPaint().getColor());
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_cap_round:
			brushListener.capChanged(Cap.ROUND);
			brushTipView.setShape(Shape.CIRCLE);
			break;
		case R.id.btn_cap_squared:
			brushListener.capChanged(Cap.SQUARE);
			brushTipView.setShape(Shape.RECT);
			break;
		default:
			dismiss();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		final float percent = (float) progress / (float) seekBar.getMax();
		final int strokeWidth = Math.round(maxStrokeWidth * percent);
		final BrushTipView colorPickerView = (BrushTipView) findViewById(R.id.view_brushtip);
		colorPickerView.setShapeRadius(strokeWidth / 2);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		final float percent = (float) seekBar.getProgress() / (float) seekBar.getMax();
		final int strokeWidth = Math.round(maxStrokeWidth * percent);
		brushListener.strokeChanged(strokeWidth);
	}

	@Override
	public void onShapeClicked() {
		dismiss();
	}
}
