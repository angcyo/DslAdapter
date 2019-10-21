package com.angcyo.dsladapter

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Rect
import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */


public fun RecyclerView.eachChildRViewHolder(
    targetView: View? = null,/*指定目标, 则只回调目标前后的ViewHolder*/
    callback: (
        beforeViewHolder: DslViewHolder?,
        viewHolder: DslViewHolder,
        afterViewHolder: DslViewHolder?
    ) -> Unit
) {

    val childCount = childCount
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        val childViewHolder = findContainingViewHolder(child)

        childViewHolder?.let {

            //前一个child
            var beforeViewHolder: DslViewHolder? = null
            //后一个child
            var afterViewHolder: DslViewHolder? = null

            if (i >= 1) {
                beforeViewHolder = findContainingViewHolder(getChildAt(i - 1)) as DslViewHolder?
            }
            if (i < childCount - 1) {
                afterViewHolder = findContainingViewHolder(getChildAt(i + 1)) as DslViewHolder?
            }

            if (targetView != null) {
                if (targetView == child) {
                    callback.invoke(beforeViewHolder, it as DslViewHolder, afterViewHolder)
                    return
                }
            } else {
                callback.invoke(beforeViewHolder, it as DslViewHolder, afterViewHolder)
            }
        }
    }
}

/**
 * 获取View, 相对于手机屏幕的矩形
 * */
public fun View.getViewRect(result: Rect = Rect()): Rect {
    var offsetX = 0
    var offsetY = 0

    //横屏, 并且显示了虚拟导航栏的时候. 需要左边偏移
    //只计算一次
    (context as? Activity)?.let {
        it.window.decorView.getGlobalVisibleRect(result)
        if (result.width() > result.height()) {
            //横屏了
            offsetX = navBarHeight(it)
        }
    }

    return getViewRect(offsetX, offsetY, result)
}

/**
 * 获取View, 相对于手机屏幕的矩形, 带皮阿尼一
 * */
public fun View.getViewRect(offsetX: Int, offsetY: Int, result: Rect = Rect()): Rect {
    //可见位置的坐标, 超出屏幕的距离会被剃掉
    //getGlobalVisibleRect(r)
    val r2 = IntArray(2)
    //val r3 = IntArray(2)
    //相对于屏幕的坐标
    getLocationOnScreen(r2)
    //相对于窗口的坐标
    //getLocationInWindow(r3)

    val left = r2[0] + offsetX
    val top = r2[1] + offsetY

    result.set(left, top, left + measuredWidth, top + measuredHeight)
    return result
}

/**
 * 导航栏的高度(如果显示了)
 */
fun navBarHeight(context: Context): Int {
    var result = 0

    if (context is Activity) {
        val decorRect = Rect()
        val windowRect = Rect()

        context.window.decorView.getGlobalVisibleRect(decorRect)
        context.window.findViewById<View>(Window.ID_ANDROID_CONTENT)
            .getGlobalVisibleRect(windowRect)

        if (decorRect.width() > decorRect.height()) {
            //横屏
            result = decorRect.width() - windowRect.width()
        } else {
            //竖屏
            result = decorRect.bottom - windowRect.bottom
        }
    }

    return result
}

fun Rect.clear() {
    set(0, 0, 0, 0)
}

public fun nowTime() = System.currentTimeMillis()

public val <T> T.dp: Float by lazy {
    Resources.getSystem()?.displayMetrics?.density ?: 0f
}

public val <T> T.dpi: Int by lazy {
    Resources.getSystem()?.displayMetrics?.density?.toInt() ?: 0
}

public fun View.setHeight(height: Int) {
    val params = layoutParams
    params.height = height
    layoutParams = params
}

public fun Int.have(value: Int): Boolean = if (this == 0 || value == 0) false
else if (this == 0 && value == 0) true
else {
    ((this > 0 && value > 0) || (this < 0 && value < 0)) &&
            this and value == value
}

public fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = true): View {
    if (layoutId == -1) {
        return this
    }
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

/**
 * 设置视图的宽高
 * */
public fun View.setWidthHeight(width: Int, height: Int) {
    val params = layoutParams
    params.width = width
    params.height = height
    layoutParams = params
}