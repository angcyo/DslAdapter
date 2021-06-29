package com.angcyo.dsladapter

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
fun RecyclerView.eachChildRViewHolder(
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
fun View.getViewRect(result: Rect = Rect()): Rect {
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
fun View.getViewRect(offsetX: Int, offsetY: Int, result: Rect = Rect()): Rect {
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

fun notNull(vararg anys: Any?, doIt: (Array<Any>) -> Unit) {
    var haveNull = false

    for (any in anys) {
        if (any == null) {
            haveNull = true
            break
        }
    }

    if (!haveNull) {
        doIt(anys as Array<Any>)
    }
}

fun Rect.clear() {
    set(0, 0, 0, 0)
}

fun nowTime() = System.currentTimeMillis()

val <T> T.dp: Float by lazy {
    Resources.getSystem()?.displayMetrics?.density ?: 0f
}

val <T> T.dpi: Int by lazy {
    Resources.getSystem()?.displayMetrics?.density?.toInt() ?: 0
}

fun View.setHeight(height: Int) {
    val params = layoutParams
    params.height = height
    layoutParams = params
}

fun Int.have(value: Int): Boolean = if (this == 0 || value == 0) false
else if (this == 0 && value == 0) true
else {
    ((this > 0 && value > 0) || (this < 0 && value < 0)) &&
            this and value == value
}

fun ViewGroup.inflate(@LayoutRes layoutId: Int, attachToRoot: Boolean = true): View {
    if (layoutId == -1) {
        return this
    }
    return LayoutInflater.from(context).inflate(layoutId, this, attachToRoot)
}

/**
 * 设置视图的宽高
 * */
fun View.setWidthHeight(width: Int, height: Int) {
    val params = layoutParams
    params.width = width
    params.height = height
    layoutParams = params
}

/**快速创建网格布局*/
fun gridLayout(
    context: Context,
    dslAdapter: DslAdapter,
    spanCount: Int = 4,
    orientation: Int = RecyclerView.VERTICAL,
    reverseLayout: Boolean = false
): GridLayoutManager {
    return GridLayoutManager(
        context,
        spanCount,
        orientation,
        reverseLayout
    ).apply {
        dslSpanSizeLookup(dslAdapter)
    }
}

/**SpanSizeLookup*/
fun GridLayoutManager.dslSpanSizeLookup(dslAdapter: DslAdapter): GridLayoutManager.SpanSizeLookup {
    //设置span size
    val spanCount = spanCount
    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return when {
                dslAdapter.isAdapterStatus() -> spanCount
                else -> dslAdapter.getItemData(position)?.run {
                    if (itemSpanCount == -1) {
                        spanCount
                    } else {
                        itemSpanCount
                    }
                } ?: 1
            }
        }
    }
    this.spanSizeLookup = spanSizeLookup
    return spanSizeLookup
}

/**SpanSizeLookup*/
fun GridLayoutManager.dslSpanSizeLookup(recyclerView: RecyclerView): GridLayoutManager.SpanSizeLookup {
    //设置span size
    val spanCount = spanCount
    val spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            val dslAdapter = recyclerView.adapter as? DslAdapter
            return when {
                dslAdapter?.isAdapterStatus() == true -> spanCount
                else -> dslAdapter?.getItemData(position)?.run {
                    if (itemSpanCount == -1) {
                        spanCount
                    } else {
                        itemSpanCount
                    }
                } ?: 1
            }
        }
    }
    this.spanSizeLookup = spanSizeLookup
    return spanSizeLookup
}

fun View.fullSpan(full: Boolean = true) {
    val layoutParams = layoutParams
    if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
        if (full != layoutParams.isFullSpan) {
            layoutParams.isFullSpan = true
            this.layoutParams = layoutParams
        }
    }
}

/**文本的高度*/
fun Paint.textHeight(): Float = descent() - ascent()

const val FLAG_NO_INIT = -1

const val FLAG_NONE = 0

const val FLAG_ALL = ItemTouchHelper.LEFT or
        ItemTouchHelper.RIGHT or
        ItemTouchHelper.DOWN or
        ItemTouchHelper.UP

const val FLAG_VERTICAL = ItemTouchHelper.DOWN or ItemTouchHelper.UP

const val FLAG_HORIZONTAL = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

/**如果为空, 则执行[action].
 * 原样返回*/
fun <T> T?.elseNull(action: () -> Unit = {}): T? {
    if (this == null) {
        action()
    }
    return this
}

fun Any?.hash(): String? {
    return this?.hashCode()?.run { Integer.toHexString(this) }
}

fun Any.simpleHash(): String {
    return "${this.javaClass.simpleName}(${this.hash()})"
}

val undefined_size = Int.MIN_VALUE

fun View.setWidth(width: Int) {
    val params = layoutParams
    params.width = width
    layoutParams = params
}

/**设置系统背景*/
fun View.setBgDrawable(drawable: Drawable?) {
    ViewCompat.setBackground(this, drawable)
}

fun Any.className(): String {
    if (this is Class<*>) {
        return name
    }
    return this.javaClass.name
}

/**判断列表是否为空, 包括内部的数据也是非空*/
fun List<Any?>?.isListEmpty(): Boolean {
    if (this?.size ?: -1 > 0) {
        return false
    }
    return this?.run {
        find { it != null } == null
    } ?: true
}

fun View?.mH(def: Int = 0): Int {
    return this?.measuredHeight ?: def
}

fun View?.mW(def: Int = 0): Int {
    return this?.measuredWidth ?: def
}

/**[androidx/core/animation/Animator.kt:82]*/
inline fun Animator.addListener(
    crossinline onEnd: (animator: Animator) -> Unit = {},
    crossinline onStart: (animator: Animator) -> Unit = {},
    crossinline onCancel: (animator: Animator) -> Unit = {},
    crossinline onRepeat: (animator: Animator) -> Unit = {}
): Animator.AnimatorListener {
    val listener = object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animator: Animator) = onRepeat(animator)
        override fun onAnimationEnd(animator: Animator) = onEnd(animator)
        override fun onAnimationCancel(animator: Animator) = onCancel(animator)
        override fun onAnimationStart(animator: Animator) = onStart(animator)
    }
    addListener(listener)
    return listener
}