package uk.ac.cam.cl.ss958.springboard;

import android.app.Activity;

public abstract class ViewLoader {
	protected MainActivity activity;
	
	private boolean paused;
	
	public ViewLoader(MainActivity activity) {
		this.activity = activity;
	}
	
	public final void load() {
		onCreate();
		paused = true;
	}
	public final void resume() {
		onResume();
		paused = false;
	}
	
	public final void pause() {
		onPause();
		paused = true;
	}
	
	public final void relieve() {
		if(!paused) {
			onPause();
			paused = true;
		}
		onDestroy();
	}
	
	protected abstract void onCreate();
	
	protected abstract void onResume();
	
	protected abstract void onPause();
	
	protected abstract void onDestroy();
	
}
