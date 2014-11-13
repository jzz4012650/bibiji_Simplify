package com.bibizhaoji.bibiji;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.bibizhaoji.bibiji.utils.Pref;
import com.bibizhaoji.pocketsphinx.PocketSphinxService;

public class MyApp extends Application {
	public static final String TAG = MyApp.class.getSimpleName();
	private Intent intent;

	@Override
	public void onCreate() {
		super.onCreate();

		Pref.getSharePrefenrences(this);
		intent = new Intent(this, PocketSphinxService.class);
		Log.d(G.LOG_TAG, "总开关--->" + Pref.isMainSwitcherOn());
		if (Pref.isMainSwitcherOn()) {
			intent.putExtra("mainSwitcherOn", true);
		} else {
			intent.putExtra("mainSwitcherOn", false);
		}
		this.startService(intent);
	}
}
