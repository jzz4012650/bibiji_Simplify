package com.bibizhaoji.pocketsphinx;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.bibizhaoji.bibiji.G;
import com.bibizhaoji.bibiji.LockScreenActivity;
import com.bibizhaoji.bibiji.utils.Pref;

public class PocketSphinxService extends Service implements RecognitionListener {

	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	public boolean inMainActivity = false;

	private RecognizerTask recTask;
	private Thread recThread;
	private LocalBinder mBinder = new LocalBinder();

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(G.LOG_TAG, "*************Service onCreate");
		Pref.getSharePrefenrences(this);
		initRecThread();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// 如果总开关开启，则开始语音识别
		if (intent.getExtras().getBoolean("mainSwitcherOn")) {
			recTask.start();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		Log.d(G.LOG_TAG, "*************Service onDestroy");
		super.onDestroy();
		finishRecThread();
	}

	/**
	 * 识别到词语结果
	 */
	@Override
	public void onPartialResults(Bundle b) {
		final String hyp = b.getString("hyp");
		Log.d(G.LOG_TAG, "=======================>" + hyp);
		if (hyp != null && hyp.indexOf(G.REC_WORD1) != -1) {
			recTask.stop();
			jumpToActivity();
		}
	}

	/**
	 * 识别线程结束
	 */
	@Override
	public void onResults(Bundle b) {
		final String hyp = b.getString("hyp");
		Log.d(G.LOG_TAG, "|||||||||||recognizition finished:" + hyp);
	}

	/**
	 * 识别出错
	 */
	@Override
	public void onError(int err) {
		Log.d(G.LOG_TAG, "!!!!!!!!!!!PocketSphinx got an error.");
	}

	/**
	 * 跳转到指定activity
	 */
	private void jumpToActivity() {
		if (inMainActivity) {
			Log.d(G.LOG_TAG, "********主界面运行中");
			Intent i = new Intent("com.bibizhaoji.GET_REC_WORD");
			sendBroadcast(i);
		} else {
			Log.d(G.LOG_TAG, "********主界面未运行中");
			Intent i = new Intent(this, LockScreenActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(i);
		}
	}

	/**
	 * 初始化语音识别线程
	 */
	private void initRecThread() {
		recTask = new RecognizerTask(this);
		recThread = new Thread(this.recTask);
		recTask.setRecognitionListener(this);
		recThread.start();
	}

	/**
	 * 结束语音识别线程
	 */
	private void finishRecThread() {
		recTask.stop();
		recThread.interrupt();
	}

	public void startRec() {
		recTask.start();
	}

	public void stopRec() {
		recTask.stop();
	}

	// binder对象，建立与activity的联系并返回当前service对象
	public class LocalBinder extends Binder {

		public PocketSphinxService getService() {
			return PocketSphinxService.this;
		}

	}
}
