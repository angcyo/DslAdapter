package com.angcyo.dsladapter

import android.animation.ValueAnimator
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.math.MathUtils.clamp
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import com.angcyo.dsladapter.internal.SwipeMenuCallback
import java.lang.reflect.Field
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * 侧滑菜单支持助手
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class SwipeMenuHelper(var swipeMenuCallback: SwipeMenuCallback) : ItemDecoration(),
    OnChildAttachStateChangeListener {

    companion object {

        /**滑动菜单滑动方式, 默认. 固定在底部*/
        const val SWIPE_MENU_TYPE_DEFAULT = 0x1

        /**滑动菜单滑动方式, 跟随在内容后面*/
        const val SWIPE_MENU_TYPE_FLOWING = 0x2

        /**安装*/
        fun install(recyclerView: RecyclerView?): SwipeMenuHelper {
            val slideMenuHelper = SwipeMenuHelper(SwipeMenuCallback())
            slideMenuHelper.attachToRecyclerView(recyclerView)
            return slideMenuHelper
        }

        /**卸载*/
        fun uninstall(helper: SwipeMenuHelper) {
            helper.attachToRecyclerView(null)
        }
    }

    //<editor-fold desc="成员变量">

    /**当前打开的菜单ViewHolder*/
    var _swipeMenuViewHolder: RecyclerView.ViewHolder? = null

    //按下的ViewHolder
    var _downViewHolder: RecyclerView.ViewHolder? = null

    //是否需要处理事件
    var _needHandleTouch = true

    //当前正在进行左右滑or上下滑
    var _swipeFlags: Int = 0

    /**当前滚动的距离*/
    var _scrollX = 0f
    var _scrollY = 0f

    var _lastDistanceX = 0f
    var _lastDistanceY = 0f

    var _lastVelocityX = 0f
    var _lastVelocityY = 0f

    //滑动冲突
    var _dragCallbackHelper: DragCallbackHelper? = null

    //</editor-fold desc="成员变量">

    //<editor-fold desc="系统回调">

    var _recyclerView: RecyclerView? = null
    var _slop = 0

    var gestureDetectorCompat: GestureDetectorCompat? = null

    val mOnItemTouchListener = object : RecyclerView.OnItemTouchListener {
        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            if (_needHandleTouch) {
                gestureDetectorCompat?.onTouchEvent(e)
            }
            val actionMasked = e.actionMasked
            if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                touchFinish()
            }
        }

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            return when (val actionMasked = e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    _resetScrollValue()
                    gestureDetectorCompat?.onTouchEvent(e)
                }
                else -> {
                    if (_needHandleTouch) {
                        gestureDetectorCompat?.onTouchEvent(e)
                    } else {
                        if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {
                            touchFinish()
                        }
                        false
                    }
                }
            } ?: false
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            if (!disallowIntercept) {
                return
            }
        }

        fun touchFinish() {
            _dragCallbackHelper?._shouldReactToLongPress = true

            if (_needHandleTouch) {
                val downViewHolder = _downViewHolder
                val recyclerView = _recyclerView
                if (recyclerView != null && downViewHolder != null) {

                    val swipeFlag =
                        swipeMenuCallback.getMovementFlags(recyclerView, downViewHolder)

                    val swipeThreshold =
                        swipeMenuCallback.getSwipeThreshold(recyclerView, downViewHolder)

                    //左右滑动菜单
                    val swipeMaxWidth =
                        swipeMenuCallback.getSwipeMaxWidth(recyclerView, downViewHolder)
                            .toFloat()

                    val swipeMaxHeight =
                        swipeMenuCallback.getSwipeMaxHeight(recyclerView, downViewHolder)
                            .toFloat()

                    //宽度阈值
                    val swipeWidthThreshold = swipeMaxWidth * swipeThreshold
                    val swipeHeightThreshold = swipeMaxHeight * swipeThreshold

                    //速率阈值
                    val swipeVelocityXThreshold = swipeMenuCallback.getSwipeVelocityThreshold(
                        recyclerView,
                        downViewHolder,
                        _lastVelocityX
                    )
                    val swipeVelocityYThreshold = swipeMenuCallback.getSwipeVelocityThreshold(
                        recyclerView,
                        downViewHolder,
                        _lastVelocityY
                    )

                    if (_swipeFlags == DragCallbackHelper.FLAG_HORIZONTAL) {
                        if (_lastVelocityX != 0f && _lastVelocityX.absoluteValue >= swipeVelocityXThreshold) {
                            //fling
                            if (_scrollX < 0 && _lastVelocityX < 0 && swipeFlag.have(ItemTouchHelper.LEFT)) {
                                //向左快速fling
                                scrollSwipeMenuTo(downViewHolder, -swipeMaxWidth, 0f)
                            } else if (_scrollX > 0 &&
                                _lastVelocityX > 0 &&
                                swipeFlag.have(ItemTouchHelper.RIGHT)
                            ) {
                                scrollSwipeMenuTo(downViewHolder, swipeMaxWidth, 0f)
                            } else {
                                closeSwipeMenu(downViewHolder)
                            }
                        } else {
                            //scroll
                            if (_scrollX < 0) {
                                if ((_lastDistanceX > 0 && _scrollX.absoluteValue >= swipeWidthThreshold) ||
                                    (_lastDistanceX < 0 && (swipeMaxWidth + _scrollX) < swipeWidthThreshold)
                                ) {
                                    //意图打开右边的菜单
                                    scrollSwipeMenuTo(downViewHolder, -swipeMaxWidth, 0f)
                                } else {
                                    //关闭菜单
                                    closeSwipeMenu(downViewHolder)
                                }
                            } else if (_scrollX > 0) {
                                if ((_lastDistanceX < 0 && _scrollX.absoluteValue >= swipeWidthThreshold) ||
                                    (_lastDistanceX > 0 && (swipeMaxWidth - _scrollX) < swipeWidthThreshold)
                                ) {
                                    //意图打开左边的菜单
                                    scrollSwipeMenuTo(downViewHolder, swipeMaxWidth, 0f)
                                } else {
                                    //关闭菜单
                                    closeSwipeMenu(downViewHolder)
                                }
                            }
                        }
                    } else if (_swipeFlags == DragCallbackHelper.FLAG_VERTICAL) {
                        //上下滑动菜单
                        if (_lastVelocityY != 0f && _lastVelocityY.absoluteValue >= swipeVelocityYThreshold) {
                            //fling
                            if (_scrollY < 0 && _lastVelocityY < 0 && swipeFlag.have(ItemTouchHelper.DOWN)) {
                                //向下快速fling
                                scrollSwipeMenuTo(downViewHolder, 0f, swipeMaxHeight)
                            } else if (_scrollY > 0 &&
                                _lastVelocityY > 0 &&
                                swipeFlag.have(ItemTouchHelper.UP)
                            ) {
                                scrollSwipeMenuTo(downViewHolder, 0f, -swipeMaxHeight)
                            } else {
                                closeSwipeMenu(downViewHolder)
                            }
                        } else {
                            //scroll
                            if (_scrollY < 0) {
                                if ((_lastDistanceY > 0 && _scrollY.absoluteValue >= swipeHeightThreshold) ||
                                    (_lastDistanceY < 0 && (swipeMaxHeight + _scrollY) < swipeHeightThreshold)
                                ) {
                                    //意图打开下边的菜单
                                    scrollSwipeMenuTo(downViewHolder, 0f, -swipeMaxHeight)
                                } else {
                                    //关闭菜单
                                    closeSwipeMenu(downViewHolder)
                                }
                            } else if (_scrollY > 0) {
                                if ((_lastDistanceY < 0 && _scrollY.absoluteValue >= swipeHeightThreshold) ||
                                    (_lastDistanceY > 0 && (swipeMaxHeight - _scrollY) < swipeHeightThreshold)
                                ) {
                                    //意图打开上边的菜单
                                    scrollSwipeMenuTo(downViewHolder, 0f, swipeMaxHeight)
                                } else {
                                    //关闭菜单
                                    closeSwipeMenu(downViewHolder)
                                }
                            }
                        }
                    }
                }
            }

            _downViewHolder = null
            _needHandleTouch = true
            _swipeFlags = 0
            _recyclerView?.parent?.requestDisallowInterceptTouchEvent(false)
        }
    }

    val itemTouchHelperGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            val findSwipedView = findSwipedView(e)
            if (findSwipedView == null) {
                _needHandleTouch = false
                closeSwipeMenu(_swipeMenuViewHolder)
            } else {
                findSwipedView.apply {
                    _recyclerView?.adapter?.let {
                        if (it is DslAdapter) {
                            it[adapterPosition, true, false]?.apply {
                                if (_itemSwipeMenuHelper != this@SwipeMenuHelper) {
                                    _itemSwipeMenuHelper = this@SwipeMenuHelper
                                }
                            }
                        }
                    }

                    if (_lastValueAnimator?.isRunning == true ||
                        (_downViewHolder != null && _downViewHolder != this)
                    ) {
                        //快速按下其他item
                        _needHandleTouch = false
                        //closeSwipeMenu(_downViewHolder)
                    } else {
                        _downViewHolder = this
                        //L.i("down:${this.adapterPosition}")

                        if (_swipeMenuViewHolder != null && _downViewHolder != _swipeMenuViewHolder) {
                            _needHandleTouch = false
                            closeSwipeMenu(_swipeMenuViewHolder)
                        }
                    }
                }
            }
            return super.onDown(e)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            //L.i("distanceX:$distanceX distanceY:$distanceY")

            val absDx: Float = abs(distanceX)
            val absDy: Float = abs(distanceY)

            if (absDx >= _slop || absDy >= _slop) {
                _dragCallbackHelper?._shouldReactToLongPress = false
                _cancelDragHelper(e1)

                if (absDx > absDy) {
                    _lastDistanceX = distanceX
                } else {
                    _lastDistanceY = distanceY
                }
            }

            _lastVelocityX = 0f
            _lastVelocityY = 0f

            val downViewHolder = _downViewHolder
            val recyclerView = _recyclerView

            if (recyclerView != null && downViewHolder != null) {
                val swipeFlag =
                    swipeMenuCallback.getMovementFlags(recyclerView, downViewHolder)
                if (swipeFlag <= 0) {
                    //当前item, 关闭了swipe
                    _needHandleTouch = false
                } else {
                    //本次滑动的意图方向
                    val flag: Int = if (absDx > absDy) {
                        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
                    } else {
                        ItemTouchHelper.UP or ItemTouchHelper.DOWN
                    }

                    if (_swipeFlags == 0) {
                        _swipeFlags = flag
                    }

                    val swipeMaxWidth =
                        swipeMenuCallback.getSwipeMaxWidth(recyclerView, downViewHolder)
                            .toFloat()
                    val swipeMaxHeight =
                        swipeMenuCallback.getSwipeMaxHeight(recyclerView, downViewHolder)
                            .toFloat()

                    _scrollX -= distanceX
                    _scrollX = clamp(_scrollX, -swipeMaxWidth, swipeMaxWidth)
                    _scrollY -= distanceY
                    _scrollY = clamp(_scrollY, -swipeMaxHeight, swipeMaxHeight)

                    if (_swipeFlags == DragCallbackHelper.FLAG_HORIZONTAL) {
                        if (swipeFlag.have(ItemTouchHelper.LEFT) ||
                            swipeFlag.have(ItemTouchHelper.RIGHT)
                        ) {
                            _scrollY = 0f
                            if (_scrollX < 0 && swipeFlag and ItemTouchHelper.LEFT == 0) {
                                //不具备向左滑动
                                _scrollX = 0f
                            } else if (_scrollX > 0 && swipeFlag and ItemTouchHelper.RIGHT == 0) {
                                //不具备向右滑动
                                _scrollX = 0f
                            } else {
                                _recyclerView?.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        } else {
                            _swipeFlags = 0
                            _needHandleTouch = false
                            _scrollX = 0f
                            if (_swipeMenuViewHolder == _downViewHolder) {
                                //已经打开了按下的菜单, 但是菜单缺没有此方向的滑动flag
                                //则关闭菜单
                                closeSwipeMenu(_swipeMenuViewHolder)
                                return _needHandleTouch
                            } else {
                                _scrollY = 0f
                            }
                        }
                    } else {
                        if (swipeFlag.have(ItemTouchHelper.UP) || swipeFlag.have(ItemTouchHelper.DOWN)) {
                            _scrollX = 0f
                            if (_scrollY < 0 && swipeFlag and ItemTouchHelper.DOWN == 0) {
                                //不具备向下滑动
                                _scrollY = 0f
                            } else if (_scrollY > 0 && swipeFlag and ItemTouchHelper.UP == 0) {
                                //不具备向上滑动
                                _scrollY = 0f
                            } else {
                                _recyclerView?.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        } else {
                            _swipeFlags = 0
                            _needHandleTouch = false
                            _scrollY = 0f
                            if (_swipeMenuViewHolder == _downViewHolder) {
                                //已经打开了按下的菜单, 但是菜单缺没有此方向的滑动flag
                                //则关闭菜单
                                closeSwipeMenu(_swipeMenuViewHolder)
                                return _needHandleTouch
                            } else {
                                _scrollX = 0f
                            }
                        }
                    }

                    swipeMenuCallback.onSwipeTo(
                        recyclerView,
                        downViewHolder,
                        _scrollX,
                        _scrollY
                    )
                }
            } else {
                _needHandleTouch = false
            }

            return _needHandleTouch
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            //L.i("velocityX:$velocityX velocityY:$velocityY")
            _lastVelocityX = velocityX
            _lastVelocityY = velocityY
            return super.onFling(e1, e2, velocityX, velocityY)
        }
    }

    var _dragCallbackHelperTouchField: Field? = null

    fun _cancelDragHelper(e1: MotionEvent) {
        _dragCallbackHelper?._itemTouchHelper?.apply {
            if (_dragCallbackHelperTouchField == null) {
                javaClass.declaredFields.forEach {
                    if (it.type.isAssignableFrom(RecyclerView.OnItemTouchListener::class.java)) {
                        _dragCallbackHelperTouchField = it
                    }
                }
            }
            _dragCallbackHelperTouchField?.let {
                it.isAccessible = true
                val touchListener: RecyclerView.OnItemTouchListener =
                    it.get(this) as RecyclerView.OnItemTouchListener

                val rv = _recyclerView
                if (rv != null) {
                    val cancelEvent = MotionEvent.obtain(e1).apply {
                        action = MotionEvent.ACTION_CANCEL
                    }
                    touchListener.onInterceptTouchEvent(rv, cancelEvent)
                    cancelEvent.recycle()
                }
            }
        }
    }

    fun _resetScrollValue() {
        _lastVelocityX = 0f
        _lastVelocityY = 0f
        _lastDistanceX = 0f
        _lastDistanceY = 0f
        _swipeFlags = 0
    }

    override fun onChildViewDetachedFromWindow(view: View) {
        _recyclerView?.apply {
            val adapter = adapter
            getChildViewHolder(view)?.apply {

                if (adapter is DslAdapter) {
                    adapter[adapterPosition, true, false]?._itemSwipeMenuHelper = null
                }

                if (this == _swipeMenuViewHolder) {
                    _resetScrollValue()
                    _scrollX = 0f
                    _scrollY = 0f
                    swipeMenuCallback.onSwipeTo(_recyclerView!!, this, 0f, 0f)
                    _swipeMenuViewHolder = null
                }
                if (this == _downViewHolder) {
                    _downViewHolder = null
                }
            }
        }
    }

    override fun onChildViewAttachedToWindow(view: View) {
        _recyclerView?.apply {
            val adapter = adapter
            getChildViewHolder(view)?.let { viewHolder ->
                if (adapter is DslAdapter) {
                    adapter[viewHolder.adapterPosition, true, false]?.apply {
                        _itemSwipeMenuHelper = this@SwipeMenuHelper
                        swipeMenuCallback.onSwipeTo(_recyclerView!!, viewHolder, 0f, 0f)
                    }
                }
            }
        }
    }

    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (_recyclerView === recyclerView) {
            return  // nothing to do
        }
        if (_recyclerView != null) {
            destroyCallbacks()
        }
        _recyclerView = recyclerView
        if (recyclerView != null) {
            setupCallbacks()
        }
    }

    private fun setupCallbacks() {
        _recyclerView?.apply {
            val vc = ViewConfiguration.get(context)
            _slop = vc.scaledTouchSlop
            addItemDecoration(this@SwipeMenuHelper)
            addOnItemTouchListener(mOnItemTouchListener)
            addOnChildAttachStateChangeListener(this@SwipeMenuHelper)
            startGestureDetection()
        }
    }

    private fun destroyCallbacks() {
        _recyclerView?.apply {
            removeItemDecoration(this@SwipeMenuHelper)
            removeOnItemTouchListener(mOnItemTouchListener)
            removeOnChildAttachStateChangeListener(this@SwipeMenuHelper)
            stopGestureDetection()
        }
    }

    private fun startGestureDetection() {
        gestureDetectorCompat =
            GestureDetectorCompat(_recyclerView!!.context, itemTouchHelperGestureListener)
        gestureDetectorCompat?.setIsLongpressEnabled(false)
    }

    private fun stopGestureDetection() {
        gestureDetectorCompat = null
    }

    //</editor-fold desc="系统回调">

    //<editor-fold desc="辅助方法">

    /**查找事件对应的[RecyclerView.ViewHolder]*/
    private fun findSwipedView(motionEvent: MotionEvent): RecyclerView.ViewHolder? {
        val child: View = findChildView(motionEvent) ?: return null
        return _recyclerView?.getChildViewHolder(child)
    }

    fun findChildView(event: MotionEvent): View? {
        val x = event.x
        val y = event.y
        return _recyclerView?.findChildViewUnder(x, y)
    }

    fun toggleSwipeMenu(viewHolder: RecyclerView.ViewHolder) {
        if (_swipeMenuViewHolder == viewHolder) {
            closeSwipeMenu(viewHolder)
        } else if (_swipeMenuViewHolder != null && _swipeMenuViewHolder != viewHolder) {
            closeSwipeMenu(_swipeMenuViewHolder)
        } else {
            openSwipeMenu(viewHolder)
        }
    }

    fun openSwipeMenu(viewHolder: RecyclerView.ViewHolder) {
        if (_swipeMenuViewHolder != null && _swipeMenuViewHolder != viewHolder) {
            closeSwipeMenu()
            return
        }
        _recyclerView?.apply {
            val swipeMaxWidth = swipeMenuCallback.getSwipeMaxWidth(this, viewHolder)
            val swipeFlag = swipeMenuCallback.getMovementFlags(this, viewHolder)
            if (swipeFlag.have(ItemTouchHelper.LEFT)) {
                scrollSwipeMenuTo(viewHolder, -swipeMaxWidth.toFloat(), 0f)
            }
        }
    }

    fun closeSwipeMenu(viewHolder: RecyclerView.ViewHolder? = _swipeMenuViewHolder) {
        viewHolder?.apply {
            scrollSwipeMenuTo(this, 0f, 0f)
        }
    }

    var _lastValueAnimator: ValueAnimator? = null

    fun scrollSwipeMenuTo(viewHolder: RecyclerView.ViewHolder, x: Float = 0f, y: Float = 0f) {
        if (_lastValueAnimator?.isRunning == true) {
            return
        }
        _recyclerView?.apply {
            val startX = _scrollX
            val startY = _scrollY

            if (x != 0f || y == 0f) {
                //将要打开的菜单
                _swipeMenuViewHolder = viewHolder
            }

            val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
            valueAnimator.addUpdateListener {
                val fraction: Float = it.animatedValue as Float
                val currentX = startX + (x - startX) * fraction
                val currentY = startY + (y - startY) * fraction

                _scrollX = currentX
                _scrollY = currentY

                swipeMenuCallback.onSwipeTo(_recyclerView!!, viewHolder, currentX, currentY)
            }
            valueAnimator.addListener(onEnd = {
                if (x == 0f && y == 0f) {
                    //关闭菜单
                    _swipeMenuViewHolder = null
                } else {
                    _swipeMenuViewHolder = viewHolder
                }
                _lastValueAnimator = null
            }, onCancel = {
                _lastValueAnimator = null
            })
            valueAnimator.duration =
                ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION.toLong()
            valueAnimator.start()
            _lastValueAnimator = valueAnimator
        }
    }

    //</editor-fold desc="辅助方法">

}