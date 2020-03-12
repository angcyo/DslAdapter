package com.angcyo.dsladapter.internal

import android.os.Handler
import android.os.Looper
import kotlin.math.max

/**
 * 短时间内, 只会执行最后一个[Runnable]的[Handler]
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/09/19
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class OnceHandler(looper: Looper = Looper.getMainLooper()) : Handler(looper) {

    companion object {
        /**节流处理, 一段时间内, 必须触发一次*/
        const val SHAKE_TYPE_THROTTLE = 1
        /**防抖处理, 超过一段时间, 必须触发一次*/
        const val SHAKE_TYPE_DEBOUNCE = 2
    }

    var shakeType: Int = SHAKE_TYPE_DEBOUNCE
        set(value) {
            val old = field
            field = value
            if (old != value) {
                clear()
            }
        }

    private var _innerDebounceRunnable: InnerRunnable? = null
    private var _innerThrottleRunnable: InnerRunnable? = null
    private var _lastThrottleTime = 0L

    fun hasCallbacks(): Boolean = _innerDebounceRunnable != null || _innerThrottleRunnable != null

    fun once(runnable: Runnable, delayMillis: Long = 0) {
        once(delayMillis, runnable)
    }

    fun once(delayMillis: Long = 0, runnable: Runnable) {
        clear()
        if (shakeType == SHAKE_TYPE_THROTTLE) {
            val nowTime = System.currentTimeMillis()
            val diffTime = nowTime - _lastThrottleTime
            //节流
            if (diffTime >= delayMillis) {
                if (post(InnerRunnable(runnable))) {
                } else {
                    runnable.run()
                }
                _lastThrottleTime = nowTime
            } else {
                InnerRunnable(runnable).apply {
                    _innerThrottleRunnable = this
                    postDelayed(this, max(delayMillis - diffTime, delayMillis))
                }
            }
        } else {
            //抖动
            InnerRunnable(runnable).apply {
                _innerDebounceRunnable = this
                postDelayed(this, delayMillis)
            }
        }
    }

    fun once(delayMillis: Long = 0, run: () -> Unit) {
        once(delayMillis, Runnable(run))
    }

    fun clear() {
        _innerDebounceRunnable?.let {
            removeCallbacks(it)
        }
        _innerThrottleRunnable?.let {
            removeCallbacks(it)
        }
        _innerDebounceRunnable = null
        _innerThrottleRunnable = null
    }

    private inner class InnerRunnable(val raw: Runnable) : Runnable {
        override fun run() {
            _lastThrottleTime = 0
            raw.run()
            clear()
        }
    }
}