package com.angcyo.dsladapter.internal

import android.os.Handler
import android.os.Looper

/**
 * 短时间内, 只会执行最后一个[Runnable]的[Handler]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/09/19
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class OnceHandler(looper: Looper = Looper.getMainLooper()) : Handler(looper) {

    private var innerRunnable: InnerRunnable? = null

    fun hasCallbacks(): Boolean = innerRunnable != null

    fun once(runnable: Runnable, delayMillis: Long = 0) {
        once(delayMillis, runnable)
    }

    fun once(delayMillis: Long = 0, runnable: Runnable) {

        clear()

        InnerRunnable(runnable).apply {
            innerRunnable = this
            postDelayed(this, delayMillis)
        }
    }

    fun once(delayMillis: Long = 0, run: () -> Unit) {
        once(delayMillis, Runnable(run))
    }

    fun clear() {
        innerRunnable?.let {
            removeCallbacks(it)
        }
        innerRunnable = null
    }

    private inner class InnerRunnable(val raw: Runnable) : Runnable {
        override fun run() {
            raw.run()
            innerRunnable = null
        }
    }
}