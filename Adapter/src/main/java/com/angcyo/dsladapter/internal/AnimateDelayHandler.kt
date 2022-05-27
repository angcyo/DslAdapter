package com.angcyo.dsladapter.internal

import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.DslAdapterItem

/**
 * 动画延迟时长计算拦截器
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */
open class AnimateDelayHandler {

    /**间隔多少时长后, 重置延迟时长*/
    var animateIntervalDelayTime = 160L

    /**动画延迟递延的时长*/
    var animateDelayTime = 60L

    //最后一次动画的执行的本地时间, 如果很长
    var lastAnimateTime: Long = 0

    //最后一次需要的延迟时长
    var lastDelay: Long = 0

    /**计算延迟*/
    open fun computeAnimateDelay(item: DslAdapterItem): Long {
        if (item.itemAnimateRes != 0) {
            //开启了动画
            if (item._itemAnimateDelay == -1L) {
                //还未执行过动画
                val nowTime = System.currentTimeMillis()
                if (nowTime - lastAnimateTime > animateIntervalDelayTime) {
                    lastDelay = 0
                } else if (item.itemDslAdapter?._recyclerView?.scrollState == RecyclerView.SCROLL_STATE_SETTLING) {
                    //正在滚动中
                    lastDelay = 0
                } else {
                    lastDelay += animateDelayTime
                }
                lastAnimateTime = nowTime
                return lastDelay
            } else {
                return item._itemAnimateDelay
            }
        } else {
            return -1
        }
    }
}