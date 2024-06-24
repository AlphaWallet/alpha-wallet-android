package com.alphawallet.app.util

import timber.log.Timber

/** A Tree for using in Release build in Timber.**/
class ReleaseTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        return  // no need to log in release build
    }
}