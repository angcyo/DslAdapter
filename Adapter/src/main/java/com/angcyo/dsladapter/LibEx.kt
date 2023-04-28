package com.angcyo.dsladapter

import android.animation.Animator
import android.animation.AnimatorInflater
import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.annotation.AnimRes
import androidx.annotation.AnimatorRes
import androidx.annotation.LayoutRes
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.angcyo.dsladapter.internal.DslHierarchyChangeListenerWrap
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.math.min

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

fun View?.isVisible() = this?.visibility == View.VISIBLE

internal fun Any?.getMember(
    cls: Class<*>,
    member: String
): Any? {
    var result: Any? = null
    try {
        var cl: Class<*>? = cls
        while (cl != null) {
            try {
                val memberField = cls.getDeclaredField(member)
                //memberField.isAccessible = true
                makeAccessible(memberField)
                result = memberField[this]
                return result
            } catch (e: NoSuchFieldException) {
                cl = cl.superclass
            }
        }
    } catch (e: Exception) {
        //L.i("错误:" + cls.getSimpleName() + " ->" + e.getMessage());
    }
    return result
}

internal fun makeAccessible(field: Field) {
    if ((!Modifier.isPublic(field.modifiers) ||
                !Modifier.isPublic(field.declaringClass.modifiers) ||
                Modifier.isFinal(field.modifiers)) && !field.isAccessible
    ) {
        field.isAccessible = true
    }
}

fun View.setAnimator(animator: Animator) {
    setTag(R.id.lib_tag_animator, WeakReference(animator))
}

/**取消动画[Animator]*/
fun View.cancelAnimator() {
    val tag = getTag(R.id.lib_tag_animator)
    var animator: Animator? = null
    if (tag is WeakReference<*>) {
        val any = tag.get()
        if (any is Animator) {
            animator = any
        }
    } else if (tag is Animator) {
        animator = tag
    }
    animator?.cancel()
}

/**清空属性动画的相关属性*/
fun View.clearAnimatorProperty(
    scale: Boolean = true,
    translation: Boolean = true,
    alpha: Boolean = true
) {
    if (scale) {
        scaleX = 1f
        scaleY = 1f
    }

    if (translation) {
        translationX = 0f
        translationY = 0f
    }

    if (alpha) {
        this.alpha = 1f
    }
}

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

/**从指定资源id中, 加载动画[Animation]*/
fun animationOf(context: Context, @AnimRes id: Int): Animation? {
    try {
        if (id == 0 || id == -1) {
            return null
        }
        return AnimationUtils.loadAnimation(context, id)
    } catch (e: Exception) {
        //e.printStackTrace()
        //L.w(e.message)
        return null
    }
}

/**从指定资源id中, 加载动画[Animator]*/
fun animatorOf(context: Context, @AnimatorRes id: Int): Animator? {
    try {
        if (id == 0 || id == -1) {
            return null
        }
        return AnimatorInflater.loadAnimator(context, id)
    } catch (e: Exception) {
        //e.printStackTrace()
        //L.w(e.message)
        return null
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

fun List<*>?.size() = this?.size ?: 0

/**将指定位置的元素, 替换成新的元素.
 * 如果新的元素为空, 则仅移除旧元素*/
fun <T> MutableList<T>.replace(element: T, newElement: T?): Boolean {
    val index = indexOf(element)
    if (index != -1) {
        return if (newElement == null) {
            //remove
            remove(element)
        } else {
            set(index, newElement)
            true
        }
    }
    return false
}

fun View?.mH(def: Int = 0): Int {
    return this?.measuredHeight ?: def
}

fun View?.mW(def: Int = 0): Int {
    return this?.measuredWidth ?: def
}

fun View?.visible(value: Boolean = true) {
    this?.visibility = if (value) View.VISIBLE else View.GONE
}

fun View?.gone(value: Boolean = true) {
    this?.visibility = if (value) View.GONE else View.VISIBLE
}

fun View?.invisible(value: Boolean = true) {
    this?.visibility = if (value) View.INVISIBLE else View.VISIBLE
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

/**是否是主线程*/
fun isMain() = Looper.getMainLooper() == Looper.myLooper()

val RecyclerView._dslAdapter: DslAdapter? get() = adapter as? DslAdapter?

fun View.getChildOrNull(index: Int): View? {
    return if (this is ViewGroup) {
        this.getChildOrNull(index)
    } else {
        this
    }
}

/**获取指定位置[index]的[child], 如果有.*/
fun ViewGroup.getChildOrNull(index: Int): View? {
    return if (index in 0 until childCount) {
        getChildAt(index)
    } else {
        null
    }
}

fun ViewGroup.forEach(recursively: Boolean = false, map: (index: Int, child: View) -> Unit) {
    eachChild(recursively, map)
}

/**枚举所有child view
 * [recursively] 递归所有子view*/
fun ViewGroup.eachChild(recursively: Boolean = false, map: (index: Int, child: View) -> Unit) {
    for (index in 0 until childCount) {
        val childAt = getChildAt(index)
        map.invoke(index, childAt)
        if (recursively && childAt is ViewGroup) {
            childAt.eachChild(true, map)
        }
    }
}

fun ViewGroup.each(recursively: Boolean = false, map: (child: View) -> Unit) {
    eachChild(recursively) { _, child ->
        map.invoke(child)
    }
}

/**清空之前所有视图, 使用[layoutId]重新渲染*/
fun ViewGroup.replace(@LayoutRes layoutId: Int, attachToRoot: Boolean = true): View {
    if (childCount > 0 && layoutId != -1) {
        removeAllViews()
    }
    return inflate(layoutId, attachToRoot)
}

/**
 * 创建一个[MotionEvent]事件, 请主动调用 [android.view.MotionEvent.recycle]方法*/
fun motionEvent(action: Int = MotionEvent.ACTION_UP, x: Float = 0f, y: Float = 0f): MotionEvent {
    return MotionEvent.obtain(
        nowTime(),
        nowTime(),
        action,
        x,
        y,
        0
    )
}

//<editor-fold desc="Dsl吸附">

val Activity._vh: DslViewHolder
    get() = window.decorView.dslViewHolder()

val Fragment._vh: DslViewHolder?
    get() = view?.dslViewHolder()

val androidx.fragment.app.Fragment._vh: DslViewHolder?
    get() = view?.dslViewHolder()

/**从[View]中, 获取挂载的[DslViewHolder].如果没有, 则使用本身创建一个, 并设置给tag*/
fun View.dslViewHolder(): DslViewHolder {
    return this.run {
        var _tag = getTag(R.id.lib_tag_dsl_view_holder)
        if (_tag is DslViewHolder) {
            _tag
        } else {
            _tag = tag
            if (_tag is DslViewHolder) {
                _tag
            } else {
                DslViewHolder(this).apply {
                    setDslViewHolder(this)
                }
            }
        }
    }
}

/**获取挂载的[DslViewHolder]*/
fun View?.tagDslViewHolder(): DslViewHolder? {
    return this?.run {
        var _tag = getTag(R.id.lib_tag_dsl_view_holder)
        if (_tag is DslViewHolder) {
            _tag
        } else {
            _tag = tag
            if (_tag is DslViewHolder) {
                _tag
            } else {
                null
            }
        }
    }
}

/**获取挂载的[DslAdapterItem]*/
fun View?.tagDslAdapterItem(): DslAdapterItem? {
    return this?.run {
        val tag = getTag(R.id.lib_tag_dsl_adapter_item)
        if (tag is DslAdapterItem) {
            tag
        } else {
            null
        }
    }
}

/**设置挂载[DslViewHolder]*/
fun View?.setDslViewHolder(dslViewHolder: DslViewHolder?) {
    this?.setTag(R.id.lib_tag_dsl_view_holder, dslViewHolder)
}

/**设置挂载[DslAdapterItem]*/
fun View?.setDslAdapterItem(dslAdapterItem: DslAdapterItem?) {
    this?.setTag(R.id.lib_tag_dsl_adapter_item, dslAdapterItem)
}

fun View?.setDslAdapterItemDecoration(dslAdapterItem: DslAdapterItem?) {
    this?.setTag(
        R.id.lib_tag_dsl_item_decoration,
        "${dslAdapterItem?.itemTag ?: dslAdapterItem?.hashCode()}"
    )
}

//</editor-fold desc="Dsl吸附">

//</editor-fold desc="DslAdapterItem操作">

fun ViewGroup.appendDslItem(
    items: List<DslAdapterItem>,
    index: Int = -1,
    payloads: List<Any> = emptyList()
) {
    var newIndex = index
    items.forEach {
        appendDslItem(it, newIndex, payloads)
        if (newIndex >= 0) {
            newIndex++
        }
    }
}

fun ViewGroup.appendDslItem(
    dslAdapterItem: DslAdapterItem,
    index: Int = -1,
    payloads: List<Any> = emptyList()
): DslViewHolder {
    return addDslItem(dslAdapterItem, index, payloads)
}

fun ViewGroup.addDslItem(
    dslAdapterItem: DslAdapterItem,
    index: Int = -1,
    payloads: List<Any> = emptyList()
): DslViewHolder {
    setOnHierarchyChangeListener(DslHierarchyChangeListenerWrap())
    val visible = !dslAdapterItem.itemHidden

    val itemView = inflate(dslAdapterItem.itemLayoutId, false)
    val dslViewHolder = DslViewHolder(itemView)
    itemView.tag = dslViewHolder

    itemView.setDslViewHolder(dslViewHolder)
    itemView.setDslAdapterItem(dslAdapterItem)

    var itemIndex = if (index < 0) childCount else index

    itemView.visible(visible)
    dslAdapterItem.itemBind(dslViewHolder, itemIndex, dslAdapterItem, payloads)

    //头分割线的支持
    if (this is LinearLayout) {
        if (this.orientation == LinearLayout.VERTICAL) {
            if (dslAdapterItem.itemTopInsert > 0) {
                addView(
                    View(context).apply {
                        setDslAdapterItemDecoration(dslAdapterItem)
                        visible(visible)
                        setBackgroundColor(dslAdapterItem.itemDecorationColor)
                    },
                    itemIndex++,
                    LinearLayout.LayoutParams(-1, dslAdapterItem.itemTopInsert).apply {
                        leftMargin = dslAdapterItem.itemLeftOffset
                        rightMargin = dslAdapterItem.itemRightOffset
                    })
            }
        } else {
            if (dslAdapterItem.itemLeftInsert > 0) {
                addView(
                    View(context).apply {
                        setDslAdapterItemDecoration(dslAdapterItem)
                        visible(visible)
                        setBackgroundColor(dslAdapterItem.itemDecorationColor)
                    },
                    itemIndex++,
                    LinearLayout.LayoutParams(dslAdapterItem.itemTopInsert, -1).apply {
                        topMargin = dslAdapterItem.itemTopOffset
                        bottomMargin = dslAdapterItem.itemBottomOffset
                    })
            }
        }
    }
    addView(itemView, itemIndex++)
    //尾分割线的支持
    if (this is LinearLayout) {
        if (this.orientation == LinearLayout.VERTICAL) {
            if (dslAdapterItem.itemBottomInsert > 0) {
                addView(
                    View(context).apply {
                        setDslAdapterItemDecoration(dslAdapterItem)
                        visible(visible)
                        setBackgroundColor(dslAdapterItem.itemDecorationColor)
                    },
                    itemIndex,
                    LinearLayout.LayoutParams(-1, dslAdapterItem.itemBottomInsert).apply {
                        leftMargin = dslAdapterItem.itemLeftOffset
                        rightMargin = dslAdapterItem.itemRightOffset
                    })
            }
        } else {
            if (dslAdapterItem.itemRightInsert > 0) {
                addView(
                    View(context).apply {
                        setDslAdapterItemDecoration(dslAdapterItem)
                        visible(visible)
                        setBackgroundColor(dslAdapterItem.itemDecorationColor)
                    },
                    itemIndex,
                    LinearLayout.LayoutParams(dslAdapterItem.itemRightInsert, -1).apply {
                        topMargin = dslAdapterItem.itemTopOffset
                        bottomMargin = dslAdapterItem.itemBottomOffset
                    })
            }
        }
    }
    return dslViewHolder
}

/**将[DslAdapterItem]绑定到[itemView]上*/
fun DslAdapterItem.bindInRootView(
    itemView: View?,
    index: Int = -1,
    payloads: List<Any> = emptyList()
): DslViewHolder? {
    if (itemView == null) {
        return null
    }
    val dslViewHolder = DslViewHolder(itemView)
    itemView.tag = dslViewHolder
    itemView.setDslViewHolder(dslViewHolder)
    itemView.setDslAdapterItem(this)
    itemBind(dslViewHolder, index, this, payloads)
    return dslViewHolder
}

fun ViewGroup.resetDslItem(item: DslAdapterItem) {
    resetDslItem(listOf(item))
}

fun ViewGroup.resetDslItem(items: List<DslAdapterItem>) {
    val childSize = childCount
    val itemSize = items.size

    //需要替换的child索引
    val replaceIndexList = mutableListOf<Int>()

    //更新已存在的Item
    for (i in 0 until min(childSize, itemSize)) {
        val childView = getChildAt(i)
        val dslItem = items[i]

        val tag = childView.getTag(R.id.tag)
        if (tag is Int && tag == dslItem.itemLayoutId) {
            //相同布局, 则使用缓存
            val dslViewHolder = childView.dslViewHolder()
            dslItem.itemBind(dslViewHolder, i, dslItem, emptyList())
        } else {
            //不同布局, 删除原先的view, 替换成新的
            replaceIndexList.add(i)
        }
    }

    //替换不相同的Item
    replaceIndexList.forEach { i ->
        val dslItem = items[i]

        removeViewAt(i)
        addDslItem(dslItem, i)
    }

    //移除多余的item
    for (i in itemSize until childSize) {
        removeViewAt(i)
    }

    //追加新的Item
    for (i in childSize until itemSize) {
        val dslItem = items[i]
        addDslItem(dslItem)
    }
}

/**查找[ViewGroup]中, 包含的[DslAdapterItem]集合*/
fun ViewGroup.findDslItemList(onlyVisible: Boolean = true): List<DslAdapterItem> {
    val result = mutableListOf<DslAdapterItem>()
    forEach { _, child ->
        val dslItem = child.tagDslAdapterItem()
        dslItem?.apply {
            if (onlyVisible) {
                if (child.isVisible()) {
                    result.add(this)
                }
            } else {
                result.add(this)
            }
        }
    }
    return result
}

/**更新所有[DslAdapterItem]在[ViewGroup]中*/
fun ViewGroup.updateAllDslItem(payloads: List<Any> = emptyList()) {
    forEach { index, child ->
        val dslItem = child.tagDslAdapterItem()
        dslItem?.also {
            it.itemBind(child.dslViewHolder(), index, it, payloads)
        }
    }
}

/**更新指定的[DslAdapterItem]在[ViewGroup]中*/
fun ViewGroup.updateDslItem(item: DslAdapterItem, payloads: List<Any> = emptyList()) {
    forEach { index, child ->
        val dslItem = child.tagDslAdapterItem()
        dslItem?.also {
            if (item == it) {
                it.itemBind(child.dslViewHolder(), index, it, payloads)
            }
        }
    }
}

/**更新或者插入指定的[DslAdapterItem]在[ViewGroup]中*/
fun ViewGroup.updateOrInsertDslItem(
    item: DslAdapterItem,
    insertIndex: Int = -1,
    payloads: List<Any> = emptyList()
) {
    var have = false
    forEach { index, child ->
        val dslItem = child.tagDslAdapterItem()
        if (item == dslItem) {
            have = true
            //更新
            item.itemBind(child.dslViewHolder(), index, item, payloads)
        }
    }
    if (!have) {
        //插入
        addDslItem(item, insertIndex, payloads)
    }
}

/**移除指定的[item]*/
fun ViewGroup.removeDslItem(item: DslAdapterItem?) {
    removeAllDslItem { index, dslAdapterItem -> dslAdapterItem == item }
}

/**移除所有符合规则的child*/
fun ViewGroup.removeAllDslItem(predicate: (Int, DslAdapterItem?) -> Boolean = { _, item -> item != null }) {

    val removeIndexList = mutableListOf<Int>()

    forEach { index, child ->
        val dslItem = child.tagDslAdapterItem()

        if (predicate(index, dslItem)) {
            removeIndexList.add(index)
        }
    }

    //移除item
    removeIndexList.reverse()
    removeIndexList.forEach {
        removeViewAt(it)
    }
}

//<editor-fold desc="DslAdapterItem操作">