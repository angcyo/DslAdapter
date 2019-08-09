package com.angcyo.dsladapter.dsl

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslAdapter : RecyclerView.Adapter<DslViewHolder>() {

    /*为了简单起见, 这里写死套路, 理论上应该用状态器管理的.*/
    var dslAdapterStatusItem = DslAdapterStatusItem()
    var dslLoadMoreItem = DslLoadMoreItem()

    private val adapterItems = mutableListOf<DslAdapterItem>()

    override fun getItemViewType(position: Int): Int {
        return if (dslAdapterStatusItem.isNoStatus()) {
            if (isLoadMorePosition(position)) {
                dslLoadMoreItem.itemLayoutId
            } else {
                adapterItems[position].itemLayoutId
            }
        } else {
            dslAdapterStatusItem.itemLayoutId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DslViewHolder {
        //viewType, 就是布局的 Id, 这是设计原则.
        val dslViewHolder: DslViewHolder
        val itemView: View = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        dslViewHolder = DslViewHolder(itemView)
        return dslViewHolder
    }

    override fun getItemCount(): Int {
        return if (dslAdapterStatusItem.isNoStatus()) {
            if (dslLoadMoreItem.itemEnableLoadMore) {
                adapterItems.size + 1
            } else {
                adapterItems.size
            }
        } else {
            1
        }
    }

    /**当前位置, 是否是加载更多的[position]*/
    private fun isLoadMorePosition(position: Int): Boolean {
        val size = adapterItems.size
        var result = false
        if (dslLoadMoreItem.itemEnableLoadMore) {
            if (position == size) {
                result = true
            }
        }
        return result
    }

    override fun onBindViewHolder(viewHolder: DslViewHolder, position: Int) {
        val dslItem = getAdapterItem(position)
        dslItem.itemDslAdapter = this
        dslItem.itemBind.invoke(viewHolder, position, dslItem)
    }

    fun getAdapterItem(position: Int): DslAdapterItem {
        val dslItem: DslAdapterItem
        if (dslAdapterStatusItem.isNoStatus()) {
            if (isLoadMorePosition(position)) {
                dslItem = dslLoadMoreItem
            } else {
                dslItem = adapterItems[position]
            }
        } else {
            dslItem = dslAdapterStatusItem
        }
        return dslItem
    }

    override fun onViewAttachedToWindow(holder: DslViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder.adapterPosition in 0 until itemCount) {
            getAdapterItem(holder.adapterPosition).onItemViewAttachedToWindow.invoke(holder)
        }
    }

    override fun onViewDetachedFromWindow(holder: DslViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder.adapterPosition in 0 until itemCount) {
            getAdapterItem(holder.adapterPosition).onItemViewDetachedToWindow.invoke(holder)
        }
    }

    /**设置[Adapter]需要显示情感图的状态*/
    fun setAdapterStatus(status: Int) {
        dslAdapterStatusItem.itemAdapterStatus = status
        notifyDataSetChanged()
    }

    fun setLoadMoreEnable(enable: Boolean = true) {
        if (dslLoadMoreItem.itemEnableLoadMore == enable) {
            return
        }
        dslLoadMoreItem.itemEnableLoadMore = enable
        if (enable) {
            notifyItemInserted(adapterItems.size)
        } else {
            notifyItemRemoved(adapterItems.size)
        }
    }

    fun setLoadMore(status: Int) {
        dslLoadMoreItem.itemLoadMoreStatus = status
        if (dslLoadMoreItem.itemEnableLoadMore) {
            notifyItemChanged(adapterItems.size)
        }
    }

    /**
     * 在最后的位置插入数据
     */
    fun addLastItem(bean: DslAdapterItem) {
        val startPosition = adapterItems.size
        adapterItems.add(bean)
        notifyItemInserted(startPosition)
    }

    fun addLastItem(bean: List<DslAdapterItem>) {
        val startPosition = adapterItems.size
        adapterItems.addAll(bean)
        notifyItemRangeInserted(startPosition, bean.size)
    }

    fun resetItem(bean: List<DslAdapterItem>) {
        adapterItems.clear()
        adapterItems.addAll(bean)
        notifyDataSetChanged()
    }
}