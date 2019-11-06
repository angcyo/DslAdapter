package com.angcyo.dsladapter

import android.graphics.Canvas
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.TextUtils
import java.util.*

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/11/05
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DragCallbackHelper : ItemTouchHelper.Callback() {

    /**支持拖拽的方向, 0表示不开启拖拽*/
    var itemDragFlag = FLAG_ALL

    /**支持滑动删除的方向, 0表示不开启滑动*/
    var itemSwipeFlag = FLAG_NONE

    /**
     * 长按是否激活拖拽,手动开始拖拽请调用[startDrag]
     * */
    var enableLongPressDrag = true

    /**是否触发过[ACTION_STATE_DRAG]*/
    var _dragHappened = false

    /**是否触发过[ACTION_STATE_SWIPE]*/
    var _swipeHappened = false

    /**
     * 返回[viewHolder] 能够支持的 [ACTION_STATE_DRAG] [ACTION_STATE_SWIPE] 方向.
     * */
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dslAdapterItem = _dslAdapter?.getItemData(viewHolder.adapterPosition)
        return dslAdapterItem?.run {
            makeMovementFlags(
                if (itemDragEnable) itemDragFlag else FLAG_NONE,
                if (itemSwipeEnable) itemSwipeFlag else FLAG_NONE
            )
        } ?: FLAG_NONE
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.adapterPosition
        val toPosition = target.adapterPosition

        //如果[viewHolder]已经移动到[target]位置, 则返回[true]
        return _dslAdapter?.run {
            //交换数据
            Collections.swap(getValidFilterDataList(), fromPosition, toPosition)
            Collections.swap(dataItems, fromPosition, toPosition)
            //更新数据列表
            _updateAdapterItems()
            //交换界面
            notifyItemMoved(fromPosition, toPosition)
            _dragHappened = true
            true
        } ?: false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        _swipeHappened = true
        _dslAdapter?.getItemData(viewHolder.adapterPosition)?.apply {
            _dslAdapter?.removeItem(this)
        }
    }

    /**是否需要激活[ACTION_STATE_SWIPE]*/
    override fun isItemViewSwipeEnabled(): Boolean {
        return itemSwipeFlag > 0
    }

    /**长按是否激活[ACTION_STATE_DRAG]*/
    override fun isLongPressDragEnabled(): Boolean {
        return enableLongPressDrag
    }

    /**[current]是否可以[ACTION_STATE_DRAG]拖拽到[target]*/
    override fun canDropOver(
        recyclerView: RecyclerView,
        current: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val currentItem = _dslAdapter?.getItemData(current.adapterPosition)
        val targetItem = _dslAdapter?.getItemData(target.adapterPosition)

        if (currentItem != null && targetItem != null) {
            return targetItem.isItemCanDropOver(currentItem)
        }

        return super.canDropOver(recyclerView, current, target)
    }

    /**[clearView]的回调*/
    var onClearView: (recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) -> Unit =
        { _, _ ->

        }

    /**如果是快速的侧滑删除, [clearView] 可能无法被执行*/
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onClearView.invoke(recyclerView, viewHolder)
    }

    /**[onSelectedChanged]的回调*/
    var onSelectedChanged: (viewHolder: RecyclerView.ViewHolder?, actionState: Int) -> Unit =
        { _, _ -> }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        onSelectedChanged.invoke(viewHolder, actionState)

        if (viewHolder != null) {
            _dragHappened = false
            _swipeHappened = false
        }
    }

    /**滑动的时候, 是否绘制提示信息*/
    var enableSwipeTip = true

    /**提示文本*/
    var swipeTipText: CharSequence = "滑动可删除"
        set(value) {
            val old = field
            field = value
            if (!TextUtils.equals(old, value)) {
                _drawText._textLayout = null
            }
        }

    var _drawText = DrawText()

    override fun onChildDraw(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        //绘制滑动删除提示
        if (enableSwipeTip && isCurrentlyActive && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {

            val itemView = viewHolder.itemView

            val x: Float = if (dX > 0) {
                //向右滑动删除
                itemView.left.toFloat()
            } else {
                //向左滑动删除
                (itemView.right - _drawText._paint.measureText(swipeTipText.toString()))
            }

            val y: Float =
                itemView.top + itemView.measuredHeight / 2 - _drawText._paint.textHeight() / 2

            canvas.save()
            canvas.translate(x, y)
            _drawText.drawText = swipeTipText
            _drawText.onDraw(canvas)
            canvas.restore()


        }
    }

    override fun onChildDrawOver(
        canvas: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder?,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDrawOver(
            canvas,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }

    /**[android.view.MotionEvent.ACTION_UP]时, 动画需要执行的时长*/
    override fun getAnimationDuration(
        recyclerView: RecyclerView,
        animationType: Int,
        animateDx: Float,
        animateDy: Float
    ): Long {
        return super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy)
    }

    var _itemTouchHelper: ItemTouchHelper? = null
    var _recyclerView: RecyclerView? = null
    val _dslAdapter: DslAdapter?
        get() = _recyclerView?.adapter as? DslAdapter

    /**安装到[RecyclerView]*/
    fun attachToRecyclerView(recyclerView: RecyclerView) {
        _recyclerView = recyclerView
        _itemTouchHelper = _itemTouchHelper ?: ItemTouchHelper(this)
        _itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    /**从[RecyclerView]卸载*/
    fun detachFromRecyclerView() {
        _recyclerView = null
        _itemTouchHelper?.attachToRecyclerView(null)
    }

    fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        _itemTouchHelper?.startDrag(viewHolder)
    }

    fun startSwipe(viewHolder: RecyclerView.ViewHolder) {
        _itemTouchHelper?.startSwipe(viewHolder)
    }
}