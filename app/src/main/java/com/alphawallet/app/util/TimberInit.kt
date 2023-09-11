package com.alphawallet.app.util

import com.alphawallet.app.BuildConfig
import timber.log.Timber

object TimberUtils
{
    @JvmStatic
    fun configTimber()
    {
        if (BuildConfig.DEBUG)
        {
            Timber.plant(Timber.DebugTree())
        }
        else
        {
            Timber.plant(ReleaseTree())
        }
    }
}
