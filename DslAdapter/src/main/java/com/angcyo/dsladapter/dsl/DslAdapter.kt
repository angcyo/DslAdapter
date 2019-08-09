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

    private val adapterItems = mutableListOf<DslAdapterItem>()

    override fun getItemViewType(position: Int): Int {
        return if (dslAdapterStatusItem.isNoStatus()) {
            adapterItems[position].itemLayoutId
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
            adapterItems.size
        } else {
            1
        }
    }

    override fun onBindViewHolder(viewHolder: DslViewHolder, position: Int) {
        val dslItem: DslAdapterItem
        if (dslAdapterStatusItem.isNoStatus()) {
            dslItem = adapterItems[position]
        } else {
            dslItem = dslAdapterStatusItem
        }
        dslItem.itemDslAdapter = this
        dslItem.itemBind.invoke(viewHolder, position, dslItem)
    }

    /**设置[Adapter]需要显示情感图的状态*/
    fun setAdapterStatus(status: Int) {
        dslAdapterStatusItem.itemAdapterStatus = status
        notifyDataSetChanged()
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