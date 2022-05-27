package com.angcyo.dsladapter.internal

import android.view.View
import com.angcyo.dsladapter.internal.ThrottleClickListener.Companion.DEFAULT_THROTTLE_INTERVAL

/**
 * 节流点击事件回调
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/01/06
 */
open class ThrottleClickListener(
    var throttleInterval: Long = DEFAULT_THROTTLE_INTERVAL,
    val throttle: (lastTime: Long, nowTime: Long, view: View) -> Boolean = { lastTime, nowTime, _ ->
        (nowTime - lastTime) < throttleInterval
    },
    val action: (View) -> Unit
) : View.OnClickListener {

    companion object {

        //节流间隔时长
        var DEFAULT_THROTTLE_INTERVAL = 400L

        var _lastThrottleClickTime = 0L
    }

    var _lastClickTime = 0L
    override fun onClick(v: View) {
        val nowTime = System.currentTimeMillis()

        if (!throttle(_lastClickTime, nowTime, v)) {
            action(v)
            _lastClickTime = nowTime
        }
    }
}

/**全局节流事件处理*/
fun View.throttleClick(interval: Long = DEFAULT_THROTTLE_INTERVAL, action: (View) -> Unit) {
    setOnClickListener(ThrottleClickListener(interval, action = action))
}

/**全局节流事件处理*/
fun throttleClick(interval: Long = DEFAULT_THROTTLE_INTERVAL, action: () -> Unit) {
    val nowTime = System.currentTimeMillis()
    if (nowTime - ThrottleClickListener._lastThrottleClickTime > interval) {
        ThrottleClickListener._lastThrottleClickTime = nowTime
        action()
    }
}