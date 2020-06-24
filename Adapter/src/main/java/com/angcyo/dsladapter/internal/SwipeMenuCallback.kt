package com.angcyo.dsladapter.internal

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.LEFT
import androidx.recyclerview.widget.ItemTouchHelper.RIGHT
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.DragCallbackHelper
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter._dslAdapter

/**
 * [LEFT] 支持从右向左滑动
 * [RIGHT] 支持从左向右滑动
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/05/09
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class SwipeMenuCallback {

    fun dslAdapterItem(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): DslAdapterItem? {
        return recyclerView._dslAdapter?.get(viewHolder.adapterPosition, true, false)
    }

    open fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return dslAdapterItem(recyclerView, viewHolder)?.run {
            ItemTouchHelper.Callback.makeMovementFlags(
                DragCallbackHelper.FLAG_NONE,
                if (itemSwipeMenuEnable) itemSwipeMenuFlag else DragCallbackHelper.FLAG_NONE
            )
        } ?: DragCallbackHelper.FLAG_NONE
    }

    /**当滑动超过了菜单的0.3倍, 视为打开/关闭*/
    open fun getSwipeThreshold(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Float {
        return 0.3f
    }

    open fun getSwipeVelocityThreshold(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        defaultValue: Float
    ): Float {
        return defaultValue
    }

    open fun getSwipeMaxWidth(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return dslAdapterItem(recyclerView, viewHolder)?.run {
            itemSwipeWidth(viewHolder as DslViewHolder)
        } ?: DragCallbackHelper.FLAG_NONE
    }

    open fun getSwipeMaxHeight(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        return dslAdapterItem(recyclerView, viewHolder)?.run {
            itemSwipeHeight(viewHolder as DslViewHolder)
        } ?: DragCallbackHelper.FLAG_NONE
    }

    open fun onSwipeTo(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        x: Float,
        y: Float
    ) {
        dslAdapterItem(recyclerView, viewHolder)?.run {
            itemSwipeMenuTo(viewHolder as DslViewHolder, x, y)
        }
    }
}