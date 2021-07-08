//package com.stream.jmxplayer.utils
//
//import android.content.Context
//import com.google.android.gms.cast.framework.CastOptions
//import com.google.android.gms.cast.framework.OptionsProvider
//import com.google.android.gms.cast.framework.SessionProvider
//import com.stream.jmxplayer.R
//
//class CastOptionProvider : OptionsProvider {
//    override fun getCastOptions(p0: Context): CastOptions {
//        return CastOptions.Builder()
//            .setReceiverApplicationId(p0.getString(R.string.cast_app_id))
//            .build()
//    }
//
//    override fun getAdditionalSessionProviders(p0: Context): MutableList<SessionProvider> {
//        
//    }
//}