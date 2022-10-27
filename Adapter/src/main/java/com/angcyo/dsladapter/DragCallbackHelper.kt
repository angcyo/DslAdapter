package com.angcyo.dsladapter

import android.graphics.Canvas
import android.graphics.Color
import android.text.TextUtils
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.internal.DrawText
import java.util.*

/**
 * 拖拽排序和侧滑删除
 *
 * https://github.com/angcyo/DslAdapter/wiki/%E6%8B%96%E6%8B%BD%E6%8E%92%E5%BA%8F%E5%92%8C%E4%BE%A7%E6%BB%91%E5%88%A0%E9%99%A4
 *
 * [com.angcyo.dsladapter.DragCallbackHelper.Companion.install]
 * [com.angcyo.dsladapter.DragCallbackHelper.startDrag]
 *
 * [onItemMoveChanged]
 * [onItemSwipeDeleted]
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/11/05
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DragCallbackHelper : ItemTouchHelper.Callback() {

    companion object {

        /**[Drag] 和 [Swipe] Flag*/
        const val FLAG_NO_INIT = -1

        /**无*/
        const val FLAG_NONE = 0

        /**全方向*/
        const val FLAG_ALL = ItemTouchHelper.LEFT or
                ItemTouchHelper.RIGHT or
                ItemTouchHelper.DOWN or
                ItemTouchHelper.UP

        /**垂直方向*/
        const val FLAG_VERTICAL = ItemTouchHelper.DOWN or ItemTouchHelper.UP

        /**水平方向*/
        const val FLAG_HORIZONTAL = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

        /**负载更新标识*/
        const val PAYLOAD_UPDATE_PART_SWIPED = 0x1_000

        /**安装*/
        fun install(
            recyclerView: RecyclerView,
            dragFlag: Int = FLAG_ALL,
            swipeFlag: Int = FLAG_NONE
        ): DragCallbackHelper {
            return DragCallbackHelper().apply {
                itemDragFlag = dragFlag
                itemSwipeFlag = swipeFlag
                attachToRecyclerView(recyclerView)
            }
        }

        /**卸载*/
        fun uninstall(dragCallbackHelper: DragCallbackHelper?) {
            dragCallbackHelper?.detachFromRecyclerView()
        }
    }

    /**支持拖拽的方向, 0表示不开启拖拽*/
    var itemDragFlag = FLAG_ALL

    /**支持滑动删除的方向, 0表示不开启滑动*/
    var itemSwipeFlag = FLAG_NONE

    /**
     * 长按是否激活拖拽,手动开始拖拽请调用[startDrag]
     * */
    var enableLongPressDrag = true

    /**[androidx.recyclerview.widget.ItemTouchHelper.ItemTouchHelperGestureListener.mShouldReactToLongPress]*/
    var _shouldReactToLongPress = true

    /**是否触发过[ACTION_STATE_DRAG]*/
    var _dragHappened = false

    /**是否触发过[ACTION_STATE_SWIPE]*/
    var _swipeHappened = false

    /**拖拽排序后的回调, 数据源可以是[DslAdapter]中的[headerItems] [dataItems] [footerItems]其中之一
     * [fromList] [fromPosition]所在的数据源, 和对应的位置
     * [toList] [toPosition]所在的数据源, 和对应的位置
     * */
    var onItemMoveChanged: ((fromList: List<DslAdapterItem>, toList: List<DslAdapterItem>, fromPosition: Int, toPosition: Int) -> Unit)? =
        null

    /**当[item]被滑动删除后的回调
     * [item] 被删除的item*/
    var onItemSwipeDeleted: ((item: DslAdapterItem) -> Unit)? =
        null

    /**
     * 返回[viewHolder] 能够支持的 [ACTION_STATE_DRAG] [ACTION_STATE_SWIPE] 方向.
     * */
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dslAdapterItem = _dslAdapter?.getItemData(viewHolder.adapterPosition)
        return dslAdapterItem?.run {
            val dFlag =
                if (itemDragFlag >= 0) itemDragFlag else this@DragCallbackHelper.itemDragFlag
            val sFlag =
                if (itemSwipeFlag >= 0) itemSwipeFlag else this@DragCallbackHelper.itemSwipeFlag
            makeMovementFlags(
                if (itemDragEnable) dFlag else FLAG_NONE,
                if (itemSwipeEnable) sFlag else FLAG_NONE
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

            val validFilterDataList = getValidFilterDataList()
            val fromItem = validFilterDataList.getOrNull(fromPosition)
            val toItem = validFilterDataList.getOrNull(toPosition)

            if (fromItem == null || toItem == null) {
                //异常操作
                false
            } else {
                val fromPair = getItemListPairByItem(fromItem)
                val toPair = getItemListPairByItem(toItem)

                //[fromPosition]所在的数据集合
                val fromList: MutableList<DslAdapterItem>? = fromPair.first

                //[toPosition]所在的数据集合
                val toList: MutableList<DslAdapterItem>? = toPair.first

                if (fromList.isNullOrEmpty() && toList.isNullOrEmpty()) {
                    false
                } else {
                    //交换数据
                    Collections.swap(validFilterDataList, fromPosition, toPosition) //界面上的集合

                    if (fromList == toList) {
                        //在同一个数据集合中
                        Collections.swap(fromList, fromPair.second, toPair.second) //数据池的集合
                    } else {
                        //不同列表中数据交换
                        val temp = fromList!![fromPair.second]
                        fromList[fromPair.second] = toList!![toPair.second]
                        toList[toPair.second] = temp
                    }

                    //更新数据列表
                    _updateAdapterItems()
                    //交换界面
                    notifyItemMoved(fromPosition, toPosition)
                    _dragHappened = true

                    onItemMoveChanged?.invoke(fromList!!, toList!!, fromPair.second, toPair.second)
                    true
                }
            }
        } ?: false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        _swipeHappened = true
        _dslAdapter?.apply {
            getItemData(viewHolder.adapterPosition)?.apply {
                removeItemFromAll(this)
                updateItemDepend(
                    FilterParams(
                        fromDslAdapterItem = this,
                        updateDependItemWithEmpty = false,
                        payload = listOf(
                            DslAdapterItem.PAYLOAD_UPDATE_PART,
                            PAYLOAD_UPDATE_PART_SWIPED
                        )
                    )
                )
                onItemSwipeDeleted?.invoke(this)
            }
        }
    }

    /**是否需要激活[ACTION_STATE_SWIPE]*/
    override fun isItemViewSwipeEnabled(): Boolean {
        return itemSwipeFlag > 0
    }

    /**长按是否激活[ACTION_STATE_DRAG]*/
    override fun isLongPressDragEnabled(): Boolean {
        return enableLongPressDrag && _shouldReactToLongPress
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

    /**
     * 先会触发[onSelectedChanged]的[ACTION_STATE_IDLE]
     * 手势释放后回调.
     * 如果是快速的侧滑删除, [clearView] 可能无法被执行*/
    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onClearView.invoke(recyclerView, viewHolder)
    }

    /**[onSelectedChanged]的回调*/
    var onSelectedChanged: (viewHolder: RecyclerView.ViewHolder?, actionState: Int) -> Unit =
        { _, _ -> }

    /**手势选中[viewHolder]状态改变时触发
     * [actionState]
     * [androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_IDLE]
     * [androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_DRAG]
     * [androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE]
     * */
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

    var _drawText = DrawText().apply {
        textPaint.color = Color.RED
        textPaint.textSize = 14 * dp
    }

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
                (itemView.right - _drawText.textPaint.measureText(swipeTipText.toString()))
            }

            val y: Float =
                itemView.top + itemView.measuredHeight / 2 - _drawText.textPaint.textHeight() / 2

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

    //<editor-fold desc="操作方法">

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

    /**主动开始拖拽*/
    fun startDrag(viewHolder: RecyclerView.ViewHolder) {
        _itemTouchHelper?.startDrag(viewHolder)
    }

    /**主动开始侧滑*/
    fun startSwipe(viewHolder: RecyclerView.ViewHolder) {
        _itemTouchHelper?.startSwipe(viewHolder)
    }

    //</editor-fold desc="操作方法">
}