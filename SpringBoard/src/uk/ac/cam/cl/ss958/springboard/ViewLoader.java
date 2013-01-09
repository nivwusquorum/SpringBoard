package uk.ac.cam.cl.ss958.springboard;

import android.app.Activity;

public abstract class ViewLoader {
	protected MainActivity activity;
	
	private boolean paused;
	
	public ViewLoader(MainActivity activity) {
		this.activity = activity;
	}
	
	public void load() {
		onCreate();
		paused = true;
	}
	public void resume() {
		onResume();
		paused = false;
	}
	
	public void pause() {
		onPause();
		paused = true;
	}
	
	public void relieve() {
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
