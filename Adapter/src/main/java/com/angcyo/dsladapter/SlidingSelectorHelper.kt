package com.angcyo.dsladapter

import android.animation.TimeInterpolator
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.OPTION_DESELECT
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.OPTION_SELECT
import kotlin.math.max

/**
 * 滑动选择, 实现类
 *
 * https://github.com/angcyo/DslAdapter/wiki/%E5%8D%95%E9%80%89-%E5%A4%9A%E9%80%89-%E6%BB%91%E5%8A%A8%E9%80%89%E6%8B%A9
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/19
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class SlidingSelectorHelper(val context: Context, val dslAdapter: DslAdapter) :
    RecyclerView.SimpleOnItemTouchListener() {

    companion object {

        /**安装*/
        fun install(recyclerView: RecyclerView, dslAdapter: DslAdapter): SlidingSelectorHelper {
            val slidingSelectorHelper = SlidingSelectorHelper(
                recyclerView.context.applicationContext,
                dslAdapter
            )
            recyclerView.addOnItemTouchListener(slidingSelectorHelper)
            return slidingSelectorHelper
        }

        /**卸载*/
        fun uninstall(recyclerView: RecyclerView, helper: OnItemTouchListener) {
            recyclerView.removeOnItemTouchListener(helper)
        }
    }

    /**功能控制开关*/
    var enableSliding = true

    /**当手指距离[RecyclerView]顶部or顶部多少距离时, 触发滚动*/
    var scrollThresholdValue = 80 * dp

    /**滚动的步进长度, 会根据离边界的距离 自动放大补偿.*/
    var scrollStepValue: Int = 3 * dpi

    /**补偿算法*/
    var scrollStepValueInterpolator: TimeInterpolator = TimeInterpolator { ratio ->
        //智能计算 scrollStepValue
        (scrollStepValue + when {
            ratio > 0.9f -> 5f
            ratio > 0.8f -> 3f
            ratio > 0.5f -> 2f
            ratio > 0.3f -> 1f
            else -> 0f
        } * scrollStepValue)
    }

    //是否处于长按状态, 会激活滑动选择
    var _isLongPress = false

    var _recyclerView: RecyclerView? = null

    val _gestureDetector: GestureDetectorCompat by lazy {
        GestureDetectorCompat(context, _onGestureListener).apply {
            //开启长按监听
            setIsLongpressEnabled(true)
        }
    }

    val _onGestureListener: GestureDetector.OnGestureListener by lazy {
        object : GestureDetector.SimpleOnGestureListener() {

            override fun onLongPress(event: MotionEvent) {
                _isLongPress = true
                _recyclerView?.parent?.requestDisallowInterceptTouchEvent(true)
                event.action = MotionEvent.ACTION_MOVE
                _handleEvent(event)
            }
        }
    }

    val _slidingRunnable: SlidingRunnable by lazy {
        SlidingRunnable()
    }

    override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent): Boolean {
        _recyclerView = recyclerView
        //L.w(MotionEvent.actionToString(event.actionMasked) + " x:${event.x} ${event.rawX} y:${event.y} ${event.rawY}")
        _handleEvent(event)
        if (enableSliding) {
            if (_isLongPress || _gestureDetector.onTouchEvent(event)) {
                return true
            }
        }
        return false
    }

    override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
        _recyclerView = recyclerView
        //L.i(MotionEvent.actionToString(event.actionMasked) + " x:${event.x} ${event.rawX} y:${event.y} ${event.rawY}")
        _handleEvent(event)
        if (enableSliding) {
            _gestureDetector.onTouchEvent(event)
        }
    }

    var _touchX: Float = 0f
    var _touchY: Float = 0f

    fun _handleEvent(event: MotionEvent) {

        //结束操作
        if (event.actionMasked == MotionEvent.ACTION_CANCEL || event.actionMasked == MotionEvent.ACTION_UP) {
            _isLongPress = false
            _slidingRunnable._slidingDirection = 0
            _recyclerView?.parent?.requestDisallowInterceptTouchEvent(false)
        }

        //重置标志位
        if (event.actionMasked != MotionEvent.ACTION_MOVE) {
            _firstSelectorItemAdapterPosition = RecyclerView.NO_POSITION
            _lastSelectorItemAdapterPosition = RecyclerView.NO_POSITION
        }

        if (_recyclerView != null &&
            enableSliding && _isLongPress &&
            event.actionMasked == MotionEvent.ACTION_MOVE
        ) {

            _touchX = event.x
            _touchY = event.y

            val recyclerView = _recyclerView!!

            var stepValue = scrollStepValue
            val dy: Float

            if (recyclerView.measuredHeight - event.y < scrollThresholdValue) {
                //手指在RV的底部, 需要滚动了
                _slidingRunnable._slidingDirection = 1
                dy = max(recyclerView.measuredHeight - event.y, 0f)
            } else if (event.y < scrollThresholdValue) {
                //手指在RV的顶部, 需要滚动了
                _slidingRunnable._slidingDirection = -1
                dy = max(event.y, 0f)
            } else {
                _slidingRunnable._slidingDirection = 0
                dy = -1f
                _selectorItem()
            }

            //智能快进
            if (dy >= 0) {
                //智能计算 scrollStepValue
                val ratio = 1 - dy / scrollThresholdValue
                stepValue = scrollStepValueInterpolator.getInterpolation(ratio).toInt()
            }
            _slidingRunnable._scrollStepValue = stepValue
        }
    }

    //第一次批量操作的位置
    var _firstSelectorItemAdapterPosition = RecyclerView.NO_POSITION

    //最后一次选中的位置, 用来标志滑动时候的批量操作
    var _lastSelectorItemAdapterPosition = RecyclerView.NO_POSITION

    /**选择目标*/
    fun _selectorItem() {
        _recyclerView?.let { recyclerView ->
            recyclerView.findChildViewUnder(_touchX, _touchY)?.let { view ->
                recyclerView.findContainingViewHolder(view)?.let { viewHolder ->
                    val adapterPosition = viewHolder.adapterPosition

                    val selectorParams = SelectorParams(
                        dslAdapter.getItemData(viewHolder.adapterPosition),
                        updateItemDepend = true,
                        notifyWithListEmpty = false
                    )

                    if (_firstSelectorItemAdapterPosition == RecyclerView.NO_POSITION) {
                        _firstSelectorItemAdapterPosition = adapterPosition
                    }
                    if (_lastSelectorItemAdapterPosition == RecyclerView.NO_POSITION) {
                        _lastSelectorItemAdapterPosition = adapterPosition
                    }

                    //批量操作
                    if (_lastSelectorItemAdapterPosition != adapterPosition) {
                        //先批量取消, 不满足条件的
                        selectorParams.selector = OPTION_DESELECT
                        selectorParams.notifySelectListener = false
                        dslAdapter.itemSelectorHelper.selector(
                            _lastSelectorItemAdapterPosition..adapterPosition,
                            selectorParams
                        )
                    }
                    //批量选中
                    selectorParams.selector = OPTION_SELECT
                    selectorParams.notifySelectListener = true
                    dslAdapter.itemSelectorHelper.selector(
                        _firstSelectorItemAdapterPosition..adapterPosition,
                        selectorParams
                    )

                    _lastSelectorItemAdapterPosition = adapterPosition
                }
            }
        }
    }

    inner class SlidingRunnable : Runnable {
        //Negative to check scrolling up, positive to check scrolling down.
        var _slidingDirection: Int = 0
            set(value) {
                val old = field
                field = value
                if (value == 0) {
                    //取消滚动
                    _recyclerView?.removeCallbacks(this)
                } else if (old != value) {
                    _recyclerView?.removeCallbacks(this)
                    _recyclerView?.post(this)
                }
            }

        var _scrollStepValue: Int = scrollStepValue

        override fun run() {
            if (_recyclerView != null && _slidingDirection != 0) {
                _selectorItem()
                if (_slidingDirection > 0) {
                    //手指在RV底部, RV往上滚动
                    _recyclerView?.scrollBy(0, _scrollStepValue)
                } else {
                    //手指在RV顶部, RV往下滚动
                    _recyclerView?.scrollBy(0, -_scrollStepValue)
                }
                _recyclerView?.post(this)
            }
        }
    }
}