package com.angcyo.dsladapter

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlin.math.min

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

    /**包含所有[DslAdapterItem], 包括 [headerItems] [dataItems] [footerItems]的数据源*/
    val adapterItems = mutableListOf<DslAdapterItem>()

    /**底部数据, 用来存放 [DslLoadMoreItem] */
    val footerItems = mutableListOf<DslAdapterItem>()
    /**头部数据*/
    val headerItems = mutableListOf<DslAdapterItem>()
    /**列表数据*/
    val dataItems = mutableListOf<DslAdapterItem>()

    /**数据过滤规则*/
    var dslDateFilter: DslDateFilter? = DslDateFilter(this)
        set(value) {
            field = value
            updateItemDepend()
        }

    /**单/多选助手*/
    val itemSelectorHelper = ItemSelectorHelper(this)

    init {
        if (dslLoadMoreItem.itemEnableLoadMore) {
            setLoadMoreEnable(true)
        }
    }

    //<editor-fold desc="生命周期方法">

    var _recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        _recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        _recyclerView = null
    }

    override fun getItemViewType(position: Int): Int {
        return if (isAdapterStatus()) {
            dslAdapterStatusItem.itemLayoutId
        } else {
            getItemData(position)?.itemLayoutId ?: 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DslViewHolder {
        //viewType, 就是布局的 Id, 这是设计核心原则.
        val dslViewHolder: DslViewHolder
        val itemView: View = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        dslViewHolder = DslViewHolder(itemView)
        return dslViewHolder
    }

    override fun getItemCount(): Int {
        return if (isAdapterStatus()) {
            1
        } else {
            getValidFilterDataList().size
        }
    }

    override fun onBindViewHolder(viewHolder: DslViewHolder, position: Int) {
        val dslItem = getAdapterItem(position)
        dslItem.itemDslAdapter = this
        dslItem.itemBind.invoke(viewHolder, position, dslItem)
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

    //</editor-fold desc="生命周期方法">

    //<editor-fold desc="辅助方法">

    /**
     * 适配器当前是情感图状态
     * */
    fun isAdapterStatus(): Boolean {
        return !dslAdapterStatusItem.isNoStatus()
    }

    fun getAdapterItem(position: Int): DslAdapterItem {
        return if (isAdapterStatus()) {
            dslAdapterStatusItem
        } else {
            getItemData(position)!!
        }
    }

    fun _updateAdapterItems() {
        //整理数据
        adapterItems.clear()
        adapterItems.addAll(headerItems)
        adapterItems.addAll(dataItems)
        adapterItems.addAll(footerItems)
    }

    //</editor-fold desc="辅助方法">

    //<editor-fold desc="操作方法">

    /**设置[Adapter]需要显示情感图的状态*/
    fun setAdapterStatus(status: Int) {
        dslAdapterStatusItem.itemAdapterStatus = status
        notifyDataSetChanged()
    }

    fun setLoadMoreEnable(enable: Boolean = true) {
        if (dslLoadMoreItem.itemEnableLoadMore == enable &&
            getValidFilterDataList().indexOf(dslLoadMoreItem) != -1
        ) {
            return
        }
        dslLoadMoreItem.itemEnableLoadMore = enable

        changeFooterItems {
            if (enable) {
                it.add(dslLoadMoreItem)
            } else {
                it.remove(dslLoadMoreItem)
            }
        }
    }

    fun setLoadMore(status: Int) {
        dslLoadMoreItem.itemLoadMoreStatus = status
        if (dslLoadMoreItem.itemEnableLoadMore) {
            notifyItemChanged(dslLoadMoreItem)
        }
    }

    /**
     * 在最后的位置插入数据
     */
    fun addLastItem(bean: DslAdapterItem) {
        insertItem(-1, bean)
    }

    fun addLastItem(bean: List<DslAdapterItem>) {
        insertItem(-1, bean)
    }

    //修正index
    fun _validIndex(list: List<*>, index: Int): Int {
        return if (index < 0) {
            list.size
        } else {
            min(index, list.size)
        }
    }

    fun insertItem(index: Int, bean: List<DslAdapterItem>) {
        dataItems.addAll(_validIndex(dataItems, index), bean)
        _updateAdapterItems()
        updateItemDepend()
    }

    fun insertItem(index: Int, bean: DslAdapterItem) {
        dataItems.add(_validIndex(dataItems, index), bean)
        _updateAdapterItems()
        updateItemDepend()
    }

    fun resetItem(bean: List<DslAdapterItem>) {
        dataItems.clear()
        dataItems.addAll(bean)
        _updateAdapterItems()
        updateItemDepend()
    }

    /**可以在回调中改变数据, 并且会自动刷新界面*/
    fun changeItems(change: () -> Unit) {
        change()
        _updateAdapterItems()
        updateItemDepend()
    }

    fun changeDataItems(change: (dataItems: MutableList<DslAdapterItem>) -> Unit) {
        changeItems {
            change(dataItems)
        }
    }

    fun changeHeaderItems(change: (headerItems: MutableList<DslAdapterItem>) -> Unit) {
        changeItems {
            change(headerItems)
        }
    }

    fun changeFooterItems(change: (footerItems: MutableList<DslAdapterItem>) -> Unit) {
        changeItems {
            change(footerItems)
        }
    }

    /**
     * 刷新某一个item
     */
    fun notifyItemChanged(item: DslAdapterItem?) {
        notifyItemChanged(item, true)
    }

    /**支持过滤数据源*/
    fun notifyItemChanged(item: DslAdapterItem?, useFilterList: Boolean = true) {
        if (item == null) {
            return
        }
        val indexOf = getDataList(useFilterList).indexOf(item)

        if (indexOf > -1) {
            notifyItemChanged(indexOf)
        }
    }

    fun getItemData(position: Int): DslAdapterItem? {
        val list = getDataList(true)
        return if (position in list.indices) {
            list[position]
        } else {
            null
        }
    }

    /**获取数据列表*/
    fun getDataList(useFilterList: Boolean = true): List<DslAdapterItem> {
        return if (useFilterList) getValidFilterDataList() else adapterItems
    }

    /**调用[DiffUtil]更新界面*/
    fun updateItemDepend(
        filterParams: FilterParams = FilterParams(
            just = dataItems.isEmpty(),
            async = dataItems.isNotEmpty(),
            justFilter = isAdapterStatus()
        )
    ) {
        dslDateFilter?.let {
            it.updateFilterItemDepend(filterParams)
        }
    }

    /**获取有效过滤后的数据集合*/
    fun getValidFilterDataList(): List<DslAdapterItem> {
        return dslDateFilter?.filterDataList ?: adapterItems
    }

    //</editor-fold desc="操作方法">

}