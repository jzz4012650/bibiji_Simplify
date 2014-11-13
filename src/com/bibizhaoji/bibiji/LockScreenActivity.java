package com.bibizhaoji.bibiji;

import com.bibizhaoji.pocketsphinx.PocketSphinxService;
import com.bibizhaoji.pocketsphinx.PocketSphinxService.LocalBinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

/**
 * 接收到语音指令后，弹出于锁屏之上的界面
 * 
 * @author jinzhenzu
 * 
 */
public class LockScreenActivity extends Activity implements OnClickListener {

	private PocketSphinxService mService;
	
	private AnimationDrawable gifAnim;
	private MediaPlayer mediaPlayer;
	private AudioManager audioManager;
	private Button stopBtn;
	private ImageView gif;
	
	private int originalVol;
	private int maximalVol;

	@SuppressLint("InlinedApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(G.LOG_TAG, "**********LockScreenActivity.onCreate");
		
		// 设为锁屏全屏弹窗
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
						| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// 如果系统版本在JellyBean之上，隐藏虚拟按键和状态栏
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			int uiOptions;
			View decorView = getWindow().getDecorView();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			} else {
				uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
			}
			decorView.setSystemUiVisibility(uiOptions);
		}
		
		// 渲染界面
		setContentView(R.layout.activity_lock_screen);

		// 界面上控件初始化
		gif = (ImageView) findViewById(R.id.gif);
		stopBtn = (Button) findViewById(R.id.stop_btn_lockscreen);
		stopBtn.setOnClickListener(this);

	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(G.LOG_TAG, "**********LockScreenActivity.onStart");

		// 绑定service
		Intent i = new Intent(this, PocketSphinxService.class);
		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
		
		// 开始播放动画
		gifAnim = (AnimationDrawable) gif.getBackground();
		gifAnim.start();
		
		// 初始化声音播放组件
		audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		originalVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		maximalVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		playSound(G.RINGTON, G.VOLUME);
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopSound();
		
		// 重新启动语音识别并解绑service
		if (mService != null) {
			mService.startRec();
			unbindService(mConnection);
		}
	}

	/**
	 * 播放铃声(以最大音量)
	 * 
	 * @param soundResourceId
	 *            声音资源ID
	 * @param volume
	 *            音量(0.0-1.0)
	 */
	private void playSound(int soundResourceId, float volume) {
		mediaPlayer = MediaPlayer.create(this, soundResourceId);
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maximalVol, 0);
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mediaPlayer.setLooping(true);
		mediaPlayer.start();
	}

	/**
	 * 停止播放铃声
	 */
	private void stopSound() {
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
			// 恢复铃声音量
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVol, 0);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.stop_btn_lockscreen:
			stopSound();
			finish(); // 取消动画，直接退出
			break;
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.d(G.LOG_TAG, "*********service bind");
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mService.stopRec();
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			Log.d(G.LOG_TAG, "********service unbind");
//			mService.inMainActivity = false;
		}

	};
}
