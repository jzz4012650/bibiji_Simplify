package com.bibizhaoji.bibiji;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.bibizhaoji.bibiji.utils.Pref;
import com.bibizhaoji.pocketsphinx.PocketSphinxService;
import com.bibizhaoji.pocketsphinx.PocketSphinxService.LocalBinder;

public class MainActivity extends Activity implements OnClickListener {

	private PocketSphinxService mService;

	private Button stopButton;
	private Button mainSwticher;
	private Button nightModeSwitcher;
	private ImageView stateGif;
	private ImageView stateText;
	private AnimationDrawable gifAnim;
	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;
	private int originalVol;
	private int maximalVol;

	private Handler handler = new Handler();
	private ActiveReceiver mReceiver = new ActiveReceiver();

	private boolean isServiceBind = false;

	private static final int STATE_OFF = 0;
	private static final int STATE_LISTENING = 1;
	private static final int STATE_ACTIVE = 2;
	private static final int STATE_STOP = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d(G.LOG_TAG, "**********MainActivity.onCreate");

		// 初始化配置文件
		Pref.getSharePrefenrences(this);

		// 获取界面上的控件
		mainSwticher = (Button) findViewById(R.id.main_switcher);
		nightModeSwitcher = (Button) findViewById(R.id.night_mode_switcher);
		stopButton = (Button) findViewById(R.id.stop_btn);
		stateGif = (ImageView) findViewById(R.id.gif_state);
		stateText = (ImageView) findViewById(R.id.text_state);

		// 注册点击事件响应
		mainSwticher.setOnClickListener(this);
		nightModeSwitcher.setOnClickListener(this);
		stopButton.setOnClickListener(this);

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(G.LOG_TAG, "**********MainActivity.onStart");

		// 初始化总开关样式以及夜间模式按钮样式
		if (Pref.isMainSwitcherOn()) {
			mainSwticher.setBackgroundResource(R.drawable.main_switcher_on);
			setState(STATE_LISTENING);
		} else {
			mainSwticher.setBackgroundResource(R.drawable.main_switcher_off);
			setState(STATE_OFF);
		}
		nightModeSwitcher.setBackgroundResource(Pref.isNightModeOn() ? R.drawable.night_mode_on : R.drawable.night_mode_off);

		// 注册广播接收器
		IntentFilter mFilter = new IntentFilter();
		mFilter.addAction("com.bibizhaoji.GET_REC_WORD");
		registerReceiver(mReceiver, mFilter);
		
		// 初始化声音播放组件
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		originalVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		maximalVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		
		Intent i = new Intent(this, PocketSphinxService.class);
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(G.LOG_TAG, "**********MainActivity.onStop()");

		// 停止播放声音
		stopSound();
		
		// 解除广播接收器
		unregisterReceiver(mReceiver);

		// 解除与service的绑定
		if (mService != null) {
			Log.d(G.LOG_TAG, "**********call unbind");
			mService.inMainActivity = false;
			unbindService(mConnection);
			isServiceBind = false;
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		// 主服务开关
		case R.id.main_switcher:
			if (Pref.isMainSwitcherOn()) {
				Log.d(G.LOG_TAG, "关闭总开关");
				v.setBackgroundResource(R.drawable.main_switcher_off);
				Pref.setMainSwitcher(this, false);
				setState(STATE_OFF);
			} else {
				Log.d(G.LOG_TAG, "打开总开关");
				v.setBackgroundResource(R.drawable.main_switcher_on);
				Pref.setMainSwitcher(this, true);
				setState(STATE_LISTENING);
			}
			break;
		// 夜间模式开关
		case R.id.night_mode_switcher:
			if (Pref.isNightModeOn()) {
				v.setBackgroundResource(R.drawable.night_mode_off);
				Pref.setNightMode(this, false);
			} else {
				v.setBackgroundResource(R.drawable.night_mode_on);
				Pref.setNightMode(this, true);
				Intent i = new Intent(this, NightModeNoticeActivity.class);
				this.startActivity(i);
			}
			break;
		// 闭嘴按钮
		case R.id.stop_btn:
			setState(STATE_STOP);
			break;
		}

	}

	private void setState(int state) {
		switch (state) {
		case STATE_OFF:
			stateText.setBackgroundResource(R.drawable.bg_main_off);
			stateGif.setBackgroundResource(R.drawable.state_off);
			stopButton.setVisibility(View.GONE);
			if (mService != null) mService.stopRec();
			break;
		case STATE_LISTENING:
			stateText.setBackgroundResource(R.drawable.bg_main_listening);
			stateGif.setBackgroundResource(R.drawable.state_listening);
			stopButton.setVisibility(View.GONE);
			if (mService != null) mService.startRec();
			break;
		case STATE_ACTIVE:
			stateText.setBackgroundResource(R.drawable.bg_main_active);
			stateGif.setBackgroundResource(R.drawable.state_active);
			stopButton.setVisibility(View.VISIBLE);
			if (mService != null) mService.stopRec();
			playSound(G.RINGTON, G.VOLUME);
			break;
		case STATE_STOP:
			stateText.setBackgroundResource(R.drawable.bg_main_stop);
			stateGif.setBackgroundResource(R.drawable.state_stop);
			stopButton.setVisibility(View.GONE);
			stopSound();
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {
					if (Pref.isMainSwitcherOn()) {
						setState(STATE_LISTENING);
					} else {
						setState(STATE_OFF);
					}
				}
			}, G.STOP_ANIM_DURATION);
			break;
		}
		gifAnim = (AnimationDrawable) stateGif.getBackground();
		gifAnim.start();
	}

	private void playSound(int soundResourceId, float volume) {
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maximalVol, 0);
		mediaPlayer = MediaPlayer.create(this, soundResourceId);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setLooping(true);
		mediaPlayer.start();
	}

	private void stopSound() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVol, 0);
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(G.LOG_TAG, "*********service bind");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mService.inMainActivity = true;
			isServiceBind = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Log.d(G.LOG_TAG, "********service unbind");
			isServiceBind = false;
		}

	};

	private class ActiveReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			setState(STATE_ACTIVE);
		}
	}

}
