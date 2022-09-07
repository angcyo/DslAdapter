package com.angcyo.dsladapter

import android.os.Build
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.angcyo.dsladapter.internal.ThrottleClickListener
import java.lang.ref.WeakReference

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslViewHolder(
    itemView: View,
    initialCapacity: Int = DEFAULT_INITIAL_CAPACITY
) : ViewHolder(itemView) {

    companion object {
        var DEFAULT_INITIAL_CAPACITY = 32
    }

    val context get() = itemView.context

    /**
     * findViewById是循环枚举所有子View的, 多少也是消耗性能的, +一个缓存
     */
    val sparseArray: SparseArray<WeakReference<View?>> = SparseArray(initialCapacity)

    /**是否绑定过界面, 用来标识是否是首次创建布局*/
    var isBindView: Boolean = false

    /**
     * 清理缓存
     */
    fun clear() {
        sparseArray.clear()
    }

    //<editor-fold desc="事件处理">

    /**
     * 单击某个View, 有音效
     */
    fun clickView(view: View?) {
        view?.performClick()
    }

    fun clickView(@IdRes id: Int) {
        view(id)?.performClick()
    }

    /**
     * 单击某个View, 无音效
     */
    fun clickCallView(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            view?.callOnClick()
        }
    }

    fun clickCallView(@IdRes id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            view(id)?.callOnClick()
        }
    }

    fun click(@IdRes id: Int, listener: View.OnClickListener?) {
        val view = v<View>(id)
        view?.setOnClickListener(listener)
    }

    fun click(@IdRes id: Int, listener: (View) -> Unit) {
        click(id, View.OnClickListener { listener.invoke(it) })
    }

    fun click(@IdRes vararg ids: Int, listener: (View) -> Unit) {
        val clickListener = View.OnClickListener { listener.invoke(it) }
        ids.forEach {
            click(it, clickListener)
        }
    }

    fun selectorClick(
        @IdRes id: Int,
        listener: (selected: Boolean) -> Boolean = { false /*不拦截默认处理*/ }
    ) {
        click(id) {
            val old = it.isSelected
            val new = !old
            if (listener(new)) {
                //no op
            } else {
                it.isSelected = new
            }
        }
    }

    /**节流点击事件*/
    fun throttleClick(
        @IdRes id: Int,
        throttleInterval: Long = ThrottleClickListener.DEFAULT_THROTTLE_INTERVAL,
        action: (View) -> Unit
    ) {
        click(id, ThrottleClickListener(throttleInterval = throttleInterval, action = action))
    }

    /**节流点击一组事件*/
    fun throttleClick(
        @IdRes vararg ids: Int,
        throttleInterval: Long = ThrottleClickListener.DEFAULT_THROTTLE_INTERVAL,
        action: (View) -> Unit
    ) {
        val listener = ThrottleClickListener(throttleInterval = throttleInterval, action = action)
        ids.forEach {
            click(it, listener)
        }
    }

    fun clickItem(listener: View.OnClickListener?) {
        click(itemView, listener)
    }

    fun clickItem(listener: (View) -> Unit) {
        click(itemView, View.OnClickListener { listener.invoke(it) })
    }

    fun throttleClickItem(action: (View) -> Unit) {
        click(itemView, ThrottleClickListener(action = action))
    }

    fun throttleClickItem(vararg ids: Int, action: (View) -> Unit) {
        val listener = ThrottleClickListener(action = action)
        click(itemView, listener)
        ids.forEach {
            click(it, listener)
        }
    }

    fun click(view: View?, listener: View.OnClickListener?) {
        view?.setOnClickListener(listener)
    }

    fun click(view: View?, listener: (View) -> Unit) {
        view?.setOnClickListener { listener.invoke(it) }
    }

    fun longClickItem(listener: (View) -> Unit) {
        itemView.setOnLongClickListener { v ->
            listener(v)
            true
        }
    }

    fun longClick(@IdRes id: Int, listener: (View) -> Unit) {
        view(id)?.setOnLongClickListener { v ->
            listener(v)
            true
        }
    }

    fun longClick(@IdRes id: Int, listener: View.OnLongClickListener?) {
        view(id)?.setOnLongClickListener(listener)
    }

    fun longClick(view: View?, listener: View.OnClickListener?) {
        view?.setOnLongClickListener { v ->
            listener?.onClick(v)
            true
        }
    }

    fun longClick(view: View?, listener: View.OnLongClickListener?) {
        view?.setOnLongClickListener(listener)
    }

    fun check(
        @IdRes resId: Int,
        checked: Boolean,
        listener: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
    ): CompoundButton? {
        val compoundButton: CompoundButton? = v(resId)
        if (compoundButton != null) {
            compoundButton.setOnCheckedChangeListener(listener)
            compoundButton.isChecked = checked
        }
        return compoundButton
    }

    fun touch(@IdRes id: Int, block: (view: View, event: MotionEvent) -> Boolean) {
        touch(v<View>(id), block)
    }

    fun touch(view: View?, block: (view: View, event: MotionEvent) -> Boolean) {
        view?.setOnTouchListener(block)
    }

    /**初始化, 点击之后再次初始化*/
    fun clickAndInit(@IdRes id: Int, init: (View) -> Unit, click: (View) -> Unit) {
        val view = v<View>(id)
        view?.apply {
            init(this)
            setOnClickListener {
                click(it)
                init(it)
            }
        }
    }

    //</editor-fold desc="事件处理">

    //<editor-fold desc="post回调">

    fun post(runnable: Runnable) {
        itemView.post(runnable)
    }

    fun post(runnable: () -> Unit) {
        postDelay(0, runnable)
    }

    fun postDelay(runnable: Runnable, delayMillis: Long) {
        itemView.postDelayed(runnable, delayMillis)
    }

    fun postDelay(delayMillis: Long, runnable: Runnable) {
        postDelay(runnable, delayMillis)
    }

    fun postOnAnimation(runnable: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            itemView.postOnAnimation(object : Runnable {
                override fun run() {
                    runnable.invoke()
                    removeCallbacks(this)
                }
            })
        } else {
            itemView.post(object : Runnable {
                override fun run() {
                    runnable.invoke()
                    removeCallbacks(this)
                }
            })
        }
    }

    fun postDelay(delayMillis: Long, runnable: () -> Unit) {
        postDelay(object : Runnable {
            override fun run() {
                runnable.invoke()
                removeCallbacks(this)
            }

        }, delayMillis)
    }

    var _onceRunnbale: Runnable? = null

    fun postOnce(delayMillis: Long = 0, runnable: () -> Unit) {
        removeCallbacks(_onceRunnbale)
        _onceRunnbale = Runnable {
            runnable.invoke()
            removeCallbacks(_onceRunnbale)
        }
        postDelay(_onceRunnbale!!, delayMillis)
    }

    fun removeCallbacks(runnable: Runnable?) {
        itemView.removeCallbacks(runnable)
    }

    //</editor-fold desc="post回调">

    //<editor-fold desc="可见性控制">

    fun focusView(@IdRes resId: Int) {
        focus<View>(resId)
    }

    fun focused(@IdRes resId: Int) {
        focusView(resId)
    }

    /**获取焦点*/
    fun focused(view: View?) {
        view?.isFocusable = true
        view?.isFocusableInTouchMode = true
        view?.requestFocus()
    }

    fun <T : View?> focus(@IdRes resId: Int): T? {
        val v = v<View>(resId)
        if (v != null) {
            focused(v)
            return v as? T
        }
        return null
    }

    fun enable(
        @IdRes resId: Int,
        enable: Boolean = true,
        recursive: Boolean = true
    ): DslViewHolder {
        val view = v<View>(resId)
        enable(view, enable, recursive)
        return this
    }

    fun enable(view: View?, enable: Boolean, recursive: Boolean = true) {
        if (view == null) {
            return
        }
        if (view is ViewGroup && recursive) {
            for (i in 0 until view.childCount) {
                enable(view.getChildAt(i), enable, recursive)
            }
        }
        if (view.isEnabled != enable) {
            view.isEnabled = enable
        }
        if (view is EditText) {
            view.clearFocus()
        }
    }

    fun selected(@IdRes resId: Int, selected: Boolean = true): DslViewHolder {
        val view = v<View>(resId)
        selected(view, selected)
        return this
    }

    fun selected(selected: Boolean = true) {
        selected(itemView, selected)
    }

    /**选中当前的view, 以及其所有的子view*/
    fun selected(view: View?, selected: Boolean = true) {
        if (view == null) {
            return
        }
        if (view.isSelected != selected) {
            view.isSelected = selected
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                selected(view.getChildAt(i), selected)
            }
        }
    }

    fun isVisible(@IdRes resId: Int): Boolean {
        return v<View>(resId)?.visibility == View.VISIBLE
    }

    fun visible(@IdRes resId: Int): View? {
        return visible(v<View>(resId))
    }

    fun visible(@IdRes resId: Int, visible: Boolean): DslViewHolder {
        val view = v<View>(resId)
        if (visible) {
            visible(view)
        } else {
            gone(view)
        }
        return this
    }

    fun invisible(@IdRes resId: Int, invisible: Boolean): DslViewHolder {
        val view = v<View>(resId)!!
        if (invisible) {
            invisible(view)
        } else {
            visible(view)
        }
        return this
    }

    fun visible(view: View?): View? {
        if (view != null) {
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
            }
        }
        return view
    }

    fun invisible(@IdRes resId: Int): View? {
        return invisible(v<View>(resId))
    }

    fun invisible(view: View?): View? {
        if (view != null) {
            if (view.visibility != View.INVISIBLE) {
                view.clearAnimation()
                view.visibility = View.INVISIBLE
            }
        }
        return view
    }

    fun gone(@IdRes resId: Int): DslViewHolder {
        return gone(v<View>(resId))
    }

    fun gone(@IdRes resId: Int, gone: Boolean) {
        if (gone) {
            gone(v<View>(resId))
        } else {
            visible(resId)
        }
    }

    fun gone(view: View?): DslViewHolder {
        if (view != null) {
            if (view.visibility != View.GONE) {
                view.clearAnimation()
                view.visibility = View.GONE
            }
        }
        return this
    }

    fun check(@IdRes resId: Int, check: Boolean = true, notify: Boolean = true): CompoundButton? {
        return v<CompoundButton>(resId)?.apply {
            if (notify) {
                isChecked = check
            } else {
                try {
                    val mOnCheckedChangeListener =
                        getMember(CompoundButton::class.java, "mOnCheckedChangeListener")
                    setOnCheckedChangeListener(null)
                    isChecked = check
                    setOnCheckedChangeListener(mOnCheckedChangeListener as CompoundButton.OnCheckedChangeListener?)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun check(
        @IdRes resId: Int,
        check: Boolean = true,
        notify: Boolean = true,
        onCheckedChanged: (buttonView: CompoundButton, isChecked: Boolean) -> Unit
    ): CompoundButton? {
        return v<CompoundButton>(resId)?.apply {
            setOnCheckedChangeListener(onCheckedChanged)
            if (notify) {
                isChecked = check
            } else {
                try {
                    val mOnCheckedChangeListener =
                        getMember(CompoundButton::class.java, "mOnCheckedChangeListener")
                    setOnCheckedChangeListener(null)
                    isChecked = check
                    setOnCheckedChangeListener(mOnCheckedChangeListener as CompoundButton.OnCheckedChangeListener?)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    //</editor-fold desc="可见性控制">

    //<editor-fold desc="findViewById">

    fun <T : View?> v(@IdRes resId: Int): T? {
        val viewWeakReference = sparseArray[resId]
        var view: View?
        if (viewWeakReference == null) {
            view = itemView.findViewById(resId)
            sparseArray.put(resId, WeakReference(view))
        } else {
            view = viewWeakReference.get()
            if (view == null) {
                view = itemView.findViewById(resId)
                sparseArray.put(resId, WeakReference(view))
            }
        }
        return try {
            view as? T?
        } catch (e: Exception) {
            L.w(e)
            null
        }
    }

    fun tv(@IdRes resId: Int): TextView? {
        return v(resId)
    }

    fun et(@IdRes resId: Int): EditText? {
        return v(resId)
    }

    fun ev(@IdRes resId: Int): EditText? {
        return v(resId)
    }

    fun img(@IdRes resId: Int): ImageView? {
        return v(resId)
    }

    fun rv(@IdRes resId: Int): RecyclerView? {
        return v(resId)
    }

    fun group(@IdRes resId: Int): ViewGroup? {
        return v(resId)
    }

    fun group(view: View?): ViewGroup? {
        return view as? ViewGroup
    }

    fun view(@IdRes resId: Int): View? {
        return v<View>(resId)
    }

    fun cb(@IdRes resId: Int): CompoundButton? {
        return v(resId)
    }

    //</editor-fold desc="findViewById">

    //<editor-fold desc="属性控制">

    fun tag(@IdRes resId: Int, key: Int, value: Any?): Any? {
        val view = view(resId)
        val old = view?.getTag(key)
        view?.setTag(key, value)
        return old
    }

    fun isChecked(@IdRes resId: Int): Boolean {
        return cb(resId)?.isChecked == true
    }

    fun isSelected(@IdRes resId: Int): Boolean {
        return view(resId)?.isSelected == true
    }

    fun isEnabled(@IdRes resId: Int): Boolean {
        return view(resId)?.isEnabled == true
    }

    /**是否是在[RecyclerView]中*/
    fun isInRecyclerView(): Boolean {
        return itemView.parent is RecyclerView
    }

    //</editor-fold desc="属性控制">

}