package com.stream.jmxplayer.ui.fragment

import android.os.Bundle
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.stream.jmxplayer.R
import com.stream.jmxplayer.utils.GlobalFunctions.Companion.logger
import com.stream.jmxplayer.utils.SharedPreferenceUtils
import com.stream.jmxplayer.utils.ijkplayer.Settings

class SettingsFragment : PreferenceFragmentCompat() {

    //    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_settings, container, false)
//    }
    lateinit var mSettings: Settings
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        logger("sharedPref", rootKey + "")
        mSettings = Settings(requireContext())

        setPreferencesFromResource(R.xml.settings_pref, rootKey)

        val themeDropDown: DropDownPreference? = findPreference(SharedPreferenceUtils.THEME)
        themeDropDown?.summary = SharedPreferenceUtils.getUserTheme(requireContext())
        themeDropDown?.setOnPreferenceChangeListener { _, newValue ->
            logger("OnPrefChange", "$newValue")
            themeDropDown.summary = newValue as String?
            SharedPreferenceUtils.setUserTheme(requireContext(), newValue as String)
            requireActivity().recreate()
            true
        }
        val pixelFormat: DropDownPreference? =
            findPreference(getString(R.string.pref_key_pixel_format))
        pixelFormat?.value = mSettings.pixelFormat
        pixelFormat?.setOnPreferenceChangeListener { _, newValue ->
            pixelFormat.value = newValue as String?
            pixelFormat.summary = pixelFormat.entry
            mSettings.pixelFormat = newValue as String
            true
        }
        val resolution =
            findPreference<SwitchPreference>(getString(R.string.pref_key_media_codec_handle_resolution_change))
        resolution?.setDefaultValue(mSettings.mediaCodecHandleResolutionChange)
        resolution?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mSettings.mediaCodecHandleResolutionChange = newValue as Boolean
                true
            }
        val rotate =
            findPreference<SwitchPreference>(getString(R.string.pref_key_using_media_codec_auto_rotate))
        rotate?.setDefaultValue(mSettings.usingMediaCodecAutoRotate)
        rotate?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            mSettings.usingMediaCodecAutoRotate = newValue as Boolean
            true
        }
        val mediaCodec =
            findPreference<SwitchPreference>(getString(R.string.pref_key_using_media_codec))
        mediaCodec?.setDefaultValue(mSettings.usingMediaCodec)
        mediaCodec?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mSettings.usingMediaCodec = newValue as Boolean
                true
            }
        val opensles =
            findPreference<SwitchPreference>(getString(R.string.pref_key_using_opensl_es))
        opensles?.setDefaultValue(mSettings.usingOpenSLES)
        opensles?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mSettings.usingMediaCodec = newValue as Boolean
                true
            }
        val surfaceView =
            findPreference<SwitchPreference>(getString(R.string.pref_key_enable_surface_view))
        surfaceView?.setDefaultValue(mSettings.enableSurfaceView)
        surfaceView?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mSettings.enableSurfaceView = newValue as Boolean
                true
            }
        val textureView =
            findPreference<SwitchPreference>(getString(R.string.pref_key_enable_texture_view))
        textureView?.setDefaultValue(mSettings.enableSurfaceView)
        textureView?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, newValue ->
                mSettings.enableSurfaceView = newValue as Boolean
                true
            }
        val renderMode = findPreference<DropDownPreference>(getString(R.string.pref_key_render))
        textureView?.setDefaultValue()
    }
}