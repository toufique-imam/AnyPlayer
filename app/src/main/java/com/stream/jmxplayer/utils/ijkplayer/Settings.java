/*
 * Copyright (C) 2015 Bilibili
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stream.jmxplayer.utils.ijkplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.stream.jmxplayer.R;


public class Settings {
    private final Context mAppContext;
    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences.Editor editor;

    public static final int PV_PLAYER__Auto = 0;
    public static final int PV_PLAYER__AndroidMediaPlayer = 1;
    public static final int PV_PLAYER__IjkMediaPlayer = 2;
    public static final int PV_PLAYER__IjkExoMediaPlayer = 3;

    public Settings(Context context) {
        mAppContext = context.getApplicationContext();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        editor = mSharedPreferences.edit();
    }

    public boolean getEnableBackgroundPlay() {
        String key = mAppContext.getString(R.string.pref_key_enable_background_play);
        return mSharedPreferences.getBoolean(key, false);
    }

    public int getPlayer() {
        String key = mAppContext.getString(R.string.pref_key_player);
        String value = mSharedPreferences.getString(key, "");
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setPlayer(String val) {
        String key = mAppContext.getString(R.string.pref_key_player);
        editor.putString(key, val);
        editor.apply();
    }

    public int getTheme() {
        String key = mAppContext.getString(R.string.pref_key_theme);
        String val = mSharedPreferences.getString(key, "");
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            Log.e("ThemeEx", val);
            return 0;
        }
    }

    public int getThemeId() {
        int themeNow = getTheme();
        if (themeNow == 0) return R.style.Theme_JMXPlayer_NoActionBar;
        else if (themeNow == 1) return R.style.Theme_JMXPlayer_Day;
        else return R.style.Theme_JMXPlayer_Night;
    }

    public void setTheme(String val) {
        String key = mAppContext.getString(R.string.pref_key_theme);
        editor.putString(key, val);
        editor.apply();
    }

    public int getDefaultPlayer() {
        String key = mAppContext.getString(R.string.pref_key_render);
        String val = mSharedPreferences.getString(key, "");
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            Log.e("ExoEx", val);
            return 0;
        }
    }

    public void setDefaultPlayer(String value) {
        String key = mAppContext.getString(R.string.pref_key_render);
        editor.putString(key, value);
        editor.apply();
    }


    public int getRenderExo() {
        String key = mAppContext.getString(R.string.pref_key_render);
        String val = mSharedPreferences.getString(key, "");
        try {
            return Integer.parseInt(val);
        } catch (Exception ex) {
            Log.e("ExoEx", val);
            return 0;
        }
    }

    public void setRenderExo(String value) {
        String key = mAppContext.getString(R.string.pref_key_render);
        editor.putString(key, value);
        editor.apply();
    }

    public boolean getUsingMediaCodec() {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec);
        return mSharedPreferences.getBoolean(key, false);
    }

    public void setUsingMediaCodec(boolean value) {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getUsingMediaCodecAutoRotate() {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec_auto_rotate);
        return mSharedPreferences.getBoolean(key, false);
    }

    public void setUsingMediaCodecAutoRotate(boolean value) {
        String key = mAppContext.getString(R.string.pref_key_using_media_codec_auto_rotate);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getMediaCodecHandleResolutionChange() {
        String key = mAppContext.getString(R.string.pref_key_media_codec_handle_resolution_change);
        return mSharedPreferences.getBoolean(key, false);
    }

    public void setMediaCodecHandleResolutionChange(boolean value) {
        String key = mAppContext.getString(R.string.pref_key_media_codec_handle_resolution_change);
        editor.putBoolean(key, value);
        editor.apply();
        //mSharedPreferences.getBoolean(key, false);
    }

    public boolean getUsingOpenSLES() {
        String key = mAppContext.getString(R.string.pref_key_using_opensl_es);
        return mSharedPreferences.getBoolean(key, false);
    }

    public void setUsingOpenSLES(boolean value) {
        String key = mAppContext.getString(R.string.pref_key_using_opensl_es);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public String getPixelFormat() {
        String key = mAppContext.getString(R.string.pref_key_pixel_format);
        return mSharedPreferences.getString(key, "");
    }

    public void setPixelFormat(String key) {
        editor.putString(mAppContext.getString(R.string.pref_key_pixel_format), key);
        editor.apply();
    }

    public boolean getEnableNoView() {
        String key = mAppContext.getString(R.string.pref_key_enable_no_view);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getEnableSurfaceView() {
        String key = mAppContext.getString(R.string.pref_key_enable_surface_view);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getEnableTextureView() {
        String key = mAppContext.getString(R.string.pref_key_enable_texture_view);
        return mSharedPreferences.getBoolean(key, false);
    }

    public void setEnableSurfaceView(boolean value) {
        String key = mAppContext.getString(R.string.pref_key_enable_surface_view);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void setEnableTextureView(boolean value) {
        String key = mAppContext.getString(R.string.pref_key_enable_texture_view);
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getEnableDetachedSurfaceTextureView() {
        String key = mAppContext.getString(R.string.pref_key_enable_detached_surface_texture);
        return mSharedPreferences.getBoolean(key, false);
    }

    public boolean getUsingMediaDataSource() {
        String key = mAppContext.getString(R.string.pref_key_using_mediadatasource);
        return mSharedPreferences.getBoolean(key, false);
    }

    public String getLastDirectory() {
        String key = mAppContext.getString(R.string.pref_key_last_directory);
        return mSharedPreferences.getString(key, "/");
    }

    public void setLastDirectory(String path) {
        String key = mAppContext.getString(R.string.pref_key_last_directory);
        mSharedPreferences.edit().putString(key, path).apply();
    }
}
