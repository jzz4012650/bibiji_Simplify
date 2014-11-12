package com.bibizhaoji.bibiji.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 配置文件
 * 
 * @author caiyingyuan
 * */
public class Pref {

	private static SharedPreferences mPrefs;
	public static String MAIN_SWITCHER = "main_switcher";
	public static String NIGHT_MODE = "NIGHT_MODE";
	public static String FIRST_TIME = "FIRST_TIME";
	private static String BIBIJI_PREF = "BiBiJi_pref";

	private static void init(Context context) {
		mPrefs = context.getSharedPreferences(BIBIJI_PREF, android.content.Context.MODE_PRIVATE);
	}

	public static SharedPreferences getSharePrefenrences(Context context) {
		if (mPrefs == null) {
			init(context);
		}
		return mPrefs;
	}

	public static final void setFirstTime(Context context, boolean enable) {
		init(context);
		setBooleanSetting(FIRST_TIME, enable);
	}

	public static final void setMainSwitcher(Context context, boolean enable) {
		init(context);
		setBooleanSetting(MAIN_SWITCHER, enable);
	}

	public static final void setNightMode(Context context, boolean enable) {
		init(context);
		setBooleanSetting(NIGHT_MODE, enable);
	}

	public static final boolean isFirstTime() {
		return getBooleanSetting(FIRST_TIME, true);
	}

	public static final boolean isMainSwitcherOn() {
		return getBooleanSetting(MAIN_SWITCHER, false);
	}

	public static final boolean isNightModeOn() {
		return getBooleanSetting(NIGHT_MODE, false);
	}

	private static void setBooleanSetting(String key, boolean enable) {
		mPrefs.edit().putBoolean(key, enable).apply();
	}

	private static boolean getBooleanSetting(String key, boolean selected) {
		return mPrefs.getBoolean(key, selected);
	}
}
