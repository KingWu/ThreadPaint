package at.ist.tugraz.threadpaint;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ThreadPaintActivity extends Activity {
	static final String TAG = "THREADPAINT";

	private PaintView paintView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		paintView = (PaintView) findViewById(R.id.paint_view);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.clear:
				paintView.getThread().clearCanvas();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}