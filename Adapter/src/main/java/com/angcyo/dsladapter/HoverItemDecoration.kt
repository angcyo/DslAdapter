package com.angcyo.dsladapter

import android.app.Activity
import android.graphics.*
import android.view.*
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 悬停功能实现类
 *
 * https://github.com/angcyo/DslAdapter/wiki/%E6%82%AC%E5%81%9C%E6%95%88%E6%9E%9C
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/08
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class HoverItemDecoration : RecyclerView.ItemDecoration() {
    internal var recyclerView: RecyclerView? = null
    internal var hoverCallback: HoverCallback? = null
    internal var isDownInHoverItem = false

    /**分割线追加在的容器*/
    internal var windowContent: ViewGroup? = null

    /**需要移除的分割线*/
    internal val removeViews = arrayListOf<View>()

    val cancelEvent = Runnable {
        overViewHolder?.apply {

            itemView.dispatchTouchEvent(
                MotionEvent.obtain(
                    nowTime(),
                    nowTime(),
                    MotionEvent.ACTION_UP,
                    0f,
                    0f,
                    0
                )
            )
        }
    }

    internal val paint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val itemTouchListener = object : RecyclerView.SimpleOnItemTouchListener() {
        override fun onInterceptTouchEvent(
            recyclerView: RecyclerView,
            event: MotionEvent
        ): Boolean {
            val action = event.actionMasked
            if (action == MotionEvent.ACTION_DOWN) {
                isDownInHoverItem = overDecorationRect.contains(event.x.toInt(), event.y.toInt())
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                isDownInHoverItem = false
            }

            if (isDownInHoverItem) {
                //L.i("onInterceptTouchEvent:$event")
                onTouchEvent(recyclerView, event)
            }

            return isDownInHoverItem
        }

        override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
            //L.w("onTouchEvent:$event")

            if (isDownInHoverItem) {
                overViewHolder?.apply {
                    if (hoverCallback?.enableDrawableState == true) {
                        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                            //有些时候, 可能收不到 up/cancel 事件, 延时发送cancel事件, 解决一些 界面drawable的bug
                            recyclerView.postDelayed(cancelEvent, 160L)
                        } else {
                            recyclerView.removeCallbacks(cancelEvent)
                        }

                        //一定要调用dispatchTouchEvent, 否则ViewGroup里面的子View, 不会响应touchEvent
                        itemView.dispatchTouchEvent(event)
                        if (itemView is ViewGroup) {
                            if ((itemView as ViewGroup).onInterceptTouchEvent(event)) {
                                itemView.onTouchEvent(event)
                            }
                        } else {
                            itemView.onTouchEvent(event)
                        }
                    } else {
                        //没有Drawable状态, 需要手动控制touch事件, 因为系统已经管理不了 itemView了
                        if (event.actionMasked == MotionEvent.ACTION_UP) {
                            itemView.performClick()
                        }
                    }
                }
            }
        }
    }

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewDetachedFromWindow(view: View) {
            removeAllHoverView()
        }

        override fun onViewAttachedToWindow(view: View) {
        }
    }

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                //滚动状态结束
                removeAllHoverView()
            }
        }
    }

    //<editor-fold desc="操作方法">

    /**
     * 调用此方法, 安装悬浮分割线
     * */
    fun attachToRecyclerView(recyclerView: RecyclerView?, init: HoverCallback.() -> Unit = {}) {
        hoverCallback = HoverCallback()
        hoverCallback?.init()

        if (this.recyclerView !== recyclerView) {
            if (this.recyclerView != null) {
                this.destroyCallbacks()
            }

            this.recyclerView = recyclerView
            if (recyclerView != null) {
                this.setupCallbacks()
            }

            (recyclerView?.context as? Activity)?.apply {
                windowContent = window.findViewById(Window.ID_ANDROID_CONTENT)
            }
        }
    }

    /**卸载分割线*/
    fun detachedFromRecyclerView() {
        hoverCallback = null

        if (this.recyclerView != null) {
            this.destroyCallbacks()
        }
        isDownInHoverItem = false
        windowContent = null
        recyclerView = null
    }

    //</editor-fold desc="操作方法">

    private fun setupCallbacks() {
        this.recyclerView?.apply {
            addItemDecoration(this@HoverItemDecoration)
            if (hoverCallback?.enableTouchEvent == true) {
                addOnItemTouchListener(itemTouchListener)
            }
            addOnAttachStateChangeListener(attachStateChangeListener)
            addOnScrollListener(scrollListener)
        }
    }

    private fun destroyCallbacks() {
        this.recyclerView?.apply {
            removeItemDecoration(this@HoverItemDecoration)
            removeOnItemTouchListener(itemTouchListener)
            removeOnAttachStateChangeListener(attachStateChangeListener)
            removeOnScrollListener(scrollListener)
        }
        removeAllHoverView()
    }

    private fun removeAllHoverView() {
        removeViews.forEach {
            (it.parent as? ViewGroup)?.removeView(it)
        }

        removeViews.clear()
    }

    /**
     * 从Activity移除悬浮view
     * */
    private fun removeHoverView() {
        overViewHolder?.itemView?.apply {
            dispatchTouchEvent(
                MotionEvent.obtain(
                    nowTime(),
                    nowTime(),
                    MotionEvent.ACTION_CANCEL,
                    0f,
                    0f,
                    0
                )
            )
            //(parent as? ViewGroup)?.removeView(this)

            removeViews.add(this)
        }
    }

    /**
     *  添加悬浮view 到 Activity, 目的是为了 系统接管 悬浮View的touch事件以及drawable的state
     * */
    private fun addHoverView(view: View) {
        if (view.parent == null) {
            windowContent?.addView(
                view, 0,
                FrameLayout.LayoutParams(
                    overDecorationRect.width(),
                    overDecorationRect.height()
                ).apply {
                    val viewRect = recyclerView?.getViewRect()

                    leftMargin = overDecorationRect.left + (viewRect?.left ?: 0)
                    topMargin = overDecorationRect.top + (viewRect?.top ?: 0)
                }
            )
        }
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (state.isPreLayout || state.willRunSimpleAnimations()) {
            return
        }

        checkOverDecoration(parent)

        overViewHolder?.let {
            if (!overDecorationRect.isEmpty) {

                //L.d("...onDrawOverDecoration...")
                hoverCallback?.apply {
                    if (enableTouchEvent && enableDrawableState) {
                        addHoverView(it.itemView)
                    }

                    drawOverDecoration.invoke(canvas, paint, it, overDecorationRect)
                }
            }
        }
    }

    private fun childViewHolder(parent: RecyclerView, childIndex: Int): RecyclerView.ViewHolder? {
        if (parent.childCount > childIndex) {
            return parent.findContainingViewHolder(parent.getChildAt(childIndex))
        }
        return null
    }

    /**当前悬浮的分割线, 如果有.*/
    internal var overViewHolder: RecyclerView.ViewHolder? = null

    /**当前悬浮分割线的坐标.*/
    val overDecorationRect = Rect()

    /**下一个悬浮分割线的坐标.*/
    internal val nextDecorationRect = Rect()

    /**分割线的所在位置*/
    var overAdapterPosition = RecyclerView.NO_POSITION

    private var tempRect = Rect()

    /**
     * 核心方法, 用来实时监测界面上 需要浮动的 分割线.
     * */
    internal fun checkOverDecoration(parent: RecyclerView) {
        childViewHolder(parent, 0)?.let { viewHolder ->
            val firstChildAdapterPosition = viewHolder.adapterPosition

            if (firstChildAdapterPosition != RecyclerView.NO_POSITION) {

                parent.adapter?.let { adapter ->
                    hoverCallback?.let { callback ->

                        var firstChildHaveOver =
                            callback.haveOverDecoration.invoke(adapter, firstChildAdapterPosition)

                        //第一个child, 需要分割线的 position 的位置
                        var firstChildHaveOverPosition = firstChildAdapterPosition

                        if (!firstChildHaveOver) {
                            //第一个child没有分割线, 查找之前最近有分割线的position
                            val findOverPrePosition =
                                findOverPrePosition(adapter, firstChildAdapterPosition)
                            if (findOverPrePosition != RecyclerView.NO_POSITION) {
                                //找到了最近的分割线
                                firstChildHaveOver = true

                                firstChildHaveOverPosition = findOverPrePosition
                            }
                        }

                        if (firstChildHaveOver) {

                            val overStartPosition =
                                findOverStartPosition(adapter, firstChildHaveOverPosition)

                            if (overStartPosition == RecyclerView.NO_POSITION) {
                                clearOverDecoration()
                                return
                            } else {
                                firstChildHaveOverPosition = overStartPosition
                            }

                            //创建第一个位置的child 需要分割线
                            val firstViewHolder =
                                callback.createDecorationOverView.invoke(
                                    parent,
                                    adapter,
                                    firstChildHaveOverPosition
                                )

                            val overView = firstViewHolder.itemView
                            tempRect.set(
                                overView.left,
                                overView.top,
                                overView.right,
                                overView.bottom
                            )

                            val nextViewHolder =
                                findNextDecoration(parent, adapter, tempRect.height())
                            if (nextViewHolder != null) {
                                //紧挨着的下一个child也有分割线, 监测是否需要上推

                                if (callback.haveOverDecoration.invoke(
                                        adapter,
                                        nextViewHolder.adapterPosition
                                    ) &&
                                    !callback.isOverDecorationSame.invoke(
                                        adapter,
                                        firstChildAdapterPosition,
                                        nextViewHolder.adapterPosition
                                    )
                                ) {
                                    //不同的分割线, 实现上推效果
                                    if (nextViewHolder.itemView.top < tempRect.height()) {
                                        tempRect.offsetTo(
                                            0,
                                            nextViewHolder.itemView.top - tempRect.height()
                                        )
                                    }
                                }
                            }

                            if (firstChildHaveOverPosition == firstChildAdapterPosition && viewHolder.itemView.top >= 0 /*考虑分割线*/) {
                                //第一个child, 正好是 分割线的开始位置
                                clearOverDecoration()
                            } else {
                                if (overAdapterPosition != firstChildHaveOverPosition) {
                                    clearOverDecoration()

                                    overViewHolder = firstViewHolder
                                    overDecorationRect.set(tempRect)

                                    overAdapterPosition = firstChildHaveOverPosition
                                } else if (overDecorationRect != tempRect) {
                                    overDecorationRect.set(tempRect)
                                }
                            }
                        } else {
                            //当前位置不需要分割线
                            clearOverDecoration()
                        }
                    }
                }
            }
        }
    }

    /**查找下一个满足条件的分割线Holder*/
    fun findNextDecoration(
        parent: RecyclerView,
        adapter: RecyclerView.Adapter<*>,
        decorationHeight: Int,
        offsetIndex: Int = 1
    ): RecyclerView.ViewHolder? {
        var result: RecyclerView.ViewHolder? = null
        if (hoverCallback != null) {
            val callback: HoverCallback = hoverCallback!!
            val childIndex = findNextChildIndex(offsetIndex)
            if (childIndex != RecyclerView.NO_POSITION) {
                val childViewHolder = childViewHolder(parent, childIndex)
                if (childViewHolder != null) {

                    if (callback.haveOverDecoration.invoke(
                            adapter,
                            childViewHolder.adapterPosition
                        )
                    ) {
                        //如果下一个item 具有分割线
                        result = childViewHolder
                    } else {
                        //不具有分割线
                        if (childViewHolder.itemView.bottom < decorationHeight) {
                            //item的高度, 没有分割线那么高, 继续往下查找
                            result = findNextDecoration(
                                parent,
                                adapter,
                                decorationHeight,
                                offsetIndex + 1
                            )
                        } else {

                        }
                    }
                }
            }
        }
        return result
    }

    /**
     * 查找GridLayoutManager中, 下一个具有全屏样式的child索引
     * @param decorationHeight 当前已经绘制的分割线的高度, 如果下一个item的高度, 不足时, 将会继续往下查找
     * */
    fun findNextChildIndex(offsetIndex: Int = 1): Int {
        var result = offsetIndex
        result = findGridNextChildIndex(result)
        return result
    }

    fun findGridNextChildIndex(offsetIndex: Int): Int {
        var result = offsetIndex
        recyclerView?.layoutManager?.apply {
            if (this is GridLayoutManager) {

                for (i in offsetIndex until recyclerView!!.childCount) {
                    childViewHolder(recyclerView!!, i)?.let {
                        if (it.adapterPosition != RecyclerView.NO_POSITION) {
                            if (spanSizeLookup?.getSpanSize(it.adapterPosition) == this.spanCount) {
                                result = i

                                return result
                            }
                        }
                    }
                }
            }
        }
        return result
    }

    fun clearOverDecoration() {
        overDecorationRect.clear()
        nextDecorationRect.clear()
        removeHoverView()
        overViewHolder = null
        overAdapterPosition = RecyclerView.NO_POSITION
    }

    /**更新分割线*/
    fun refreshItemDecoration() {
        overAdapterPosition = RecyclerView.NO_POSITION
        recyclerView?.postInvalidate()
    }

    /**
     * 查找指定位置类型相同的分割线, 最开始的adapterPosition
     * */
    internal fun findOverStartPosition(
        adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
        adapterPosition: Int
    ): Int {
        var result = adapterPosition
        for (i in adapterPosition - 1 downTo 0) {
            if (i == 0) {
                if (hoverCallback!!.isOverDecorationSame(adapter, adapterPosition, i)) {
                    result = i
                }
                break
            } else if (!hoverCallback!!.isOverDecorationSame(adapter, adapterPosition, i)) {
                result = i + 1
                break
            }
        }

        if (result == 0) {
            hoverCallback?.let {
                if (!it.haveOverDecoration.invoke(adapter, result)) {
                    result = RecyclerView.NO_POSITION
                }
            }
        }

        return result
    }

    /**
     * 查找指定位置 没有分割线时, 最前出现分割线的adapterPosition
     * */
    internal fun findOverPrePosition(adapter: RecyclerView.Adapter<*>, adapterPosition: Int): Int {
        var result = RecyclerView.NO_POSITION
        for (i in adapterPosition - 1 downTo 0) {
            if (hoverCallback!!.haveOverDecoration.invoke(adapter, i)) {
                result = i
                break
            }
        }
        return result
    }

    class HoverCallback {

        /**激活touch手势*/
        var enableTouchEvent = true

        /**激活drawable点击效果, 此功能需要先 激活 touch 手势*/
        var enableDrawableState = enableTouchEvent

        /**
         * 当前的 位置 是否有 悬浮分割线
         * */
        var haveOverDecoration: (adapter: RecyclerView.Adapter<*>, adapterPosition: Int) -> Boolean =
            { adapter, adapterPosition ->
                if (adapter is DslAdapter) {
                    adapter.getItemData(adapterPosition)?.itemIsHover ?: false
                } else {
                    decorationOverLayoutType.invoke(adapter, adapterPosition) > 0
                }
            }

        /**
         * 根据 位置, 返回对应分割线的布局类型, 小于0, 不绘制
         *
         * @see RecyclerView.Adapter.getItemViewType
         * */
        var decorationOverLayoutType: (adapter: RecyclerView.Adapter<*>, adapterPosition: Int) -> Int =
            { adapter, adapterPosition ->
                if (adapter is DslAdapter) {
                    adapter.getItemViewType(adapterPosition)
                } else {
                    -1
                }
            }

        /**
         * 判断2个分割线是否相同, 不同的分割线, 才会悬停, 相同的分割线只会绘制一条.
         * */
        var isOverDecorationSame: (
            adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
            nowAdapterPosition: Int, nextAdapterPosition: Int
        ) -> Boolean =
            { _, _, _ ->
                false
            }

        /**
         * 创建 分割线 视图
         * */
        var createDecorationOverView: (
            recyclerView: RecyclerView,
            adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
            overAdapterPosition: Int
        ) -> RecyclerView.ViewHolder = { recyclerView, adapter, overAdapterPosition ->

            //拿到分割线对应的itemType
            val layoutType = decorationOverLayoutType.invoke(adapter, overAdapterPosition)

            //复用adapter的机制, 创建View
            val holder = adapter.createViewHolder(recyclerView, layoutType)

            //注意这里的position, 直接调用
            //[androidx.recyclerview.widget.RecyclerView.Adapter.onBindViewHolder(VH, int, java.util.List<java.lang.Object>)]
            //?
            adapter.bindViewHolder(holder, overAdapterPosition)

            //测量view
            measureHoverView.invoke(recyclerView, holder.itemView)

            holder
        }

        /**自定义layout的分割线, 不使用 adapter中的xml*/
        val customDecorationOverView: (
            recyclerView: RecyclerView,
            adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>,
            overAdapterPosition: Int
        ) -> RecyclerView.ViewHolder = { recyclerView, adapter, overAdapterPosition ->

            //拿到分割线对应的itemType
            val layoutType = decorationOverLayoutType.invoke(adapter, overAdapterPosition)

            val itemView =
                LayoutInflater.from(recyclerView.context).inflate(layoutType, recyclerView, false)

            val holder = DslViewHolder(itemView)

            //注意这里的position
            adapter.bindViewHolder(holder, overAdapterPosition)

            //测量view
            measureHoverView.invoke(recyclerView, holder.itemView)

            holder
        }

        /**
         * 测量 View, 确定宽高和绘制坐标
         * */
        var measureHoverView: (parent: RecyclerView, hoverView: View) -> Unit =
            { parent, hoverView ->
                hoverView.apply {
                    val params = layoutParams

                    val widthSize: Int
                    val widthMode: Int
                    when (params.width) {
                        -1 -> {
                            widthSize = parent.measuredWidth
                            widthMode = View.MeasureSpec.EXACTLY
                        }
                        else -> {
                            widthSize = parent.measuredWidth
                            widthMode = View.MeasureSpec.AT_MOST
                        }
                    }

                    val heightSize: Int
                    val heightMode: Int
                    when (params.height) {
                        -1 -> {
                            heightSize = parent.measuredWidth
                            heightMode = View.MeasureSpec.EXACTLY
                        }
                        else -> {
                            heightSize = parent.measuredWidth
                            heightMode = View.MeasureSpec.AT_MOST
                        }
                    }

                    //标准方法1
                    measure(
                        View.MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                        View.MeasureSpec.makeMeasureSpec(heightSize, heightMode)
                    )
                    //标准方法2
                    layout(0, 0, measuredWidth, measuredHeight)

                    //标准方法3
                    //draw(canvas)
                }
            }

        /**
         * 绘制分割线, 请不要使用 foreground 属性.
         * */
        var drawOverDecoration: (
            canvas: Canvas,
            paint: Paint,
            viewHolder: RecyclerView.ViewHolder,
            overRect: Rect
        ) -> Unit =
            { canvas, paint, viewHolder, overRect ->

                canvas.save()
                canvas.translate(overRect.left.toFloat(), overRect.top.toFloat())

                viewHolder.itemView.draw(canvas)

                if (enableDrawShadow) {
                    drawOverShadowDecoration.invoke(canvas, paint, viewHolder, overRect)
                }

                canvas.restore()
            }

        /**是否激活阴影绘制*/
        var enableDrawShadow = true

        /**
         * 绘制分割线下面的阴影, 或者其他而外的信息
         * */
        var drawOverShadowDecoration: (
            canvas: Canvas,
            paint: Paint,
            viewHolder: RecyclerView.ViewHolder,
            overRect: Rect
        ) -> Unit =
            { canvas, paint, viewHolder, overRect ->

                if (overRect.top == 0) {
                    //分割线完全显示的情况下, 才绘制阴影
                    val shadowTop = overRect.bottom.toFloat()
                    val shadowHeight = 4 * dp

                    paint.shader = LinearGradient(
                        0f, shadowTop, 0f,
                        shadowTop + shadowHeight,
                        intArrayOf(
                            Color.parseColor("#40000000"),
                            Color.TRANSPARENT /*Color.parseColor("#40000000")*/
                        ),
                        null, Shader.TileMode.CLAMP
                    )

                    //绘制阴影
                    canvas.drawRect(
                        overRect.left.toFloat(),
                        shadowTop,
                        overRect.right.toFloat(),
                        shadowTop + shadowHeight,
                        paint
                    )
                }
            }
    }
}
