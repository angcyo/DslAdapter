package com.angcyo.dsladapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.min

/**
 * https://github.com/angcyo/DslAdapter
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslAdapter : RecyclerView.Adapter<DslViewHolder>, OnDispatchUpdatesListener {

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
    var dslDataFilter: DslDataFilter? = null
        set(value) {
            if (field == value) {
                return
            }
            field?.removeDispatchUpdatesListener(this)
            field = value
            field?.addDispatchUpdatesListener(this)
            updateItemDepend()
        }

    /**单/多选助手*/
    val itemSelectorHelper = ItemSelectorHelper(this)

    /**
     * 一次性的[FilterParams], 使用完之后会被置空,调用无参[updateItemDepend]方法时使用.
     * */
    var onceFilterParams: FilterParams? = null

    /**默认的[FilterParams]*/
    var defaultFilterParams: FilterParams? = null
        get() {
            return onceFilterParams ?: (field ?: _defaultFilterParams())
        }

    constructor() : super()

    constructor(dataItems: List<DslAdapterItem>?) {
        dataItems?.let {
            this.dataItems.clear()
            this.dataItems.addAll(dataItems)
            _updateAdapterItems()
            updateItemDepend(FilterParams(async = false, just = true))
        }
    }

    init {
        dslDataFilter = DslDataFilter(this)
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

        val dslAdapterItem = when {
            isAdapterStatus() -> dslAdapterStatusItem
            holder.adapterPosition in getValidFilterDataList().indices -> getAdapterItem(holder.adapterPosition)
            else -> null
        }

        dslAdapterItem?.apply {
            holder.itemView.fullSpan(itemSpanCount == -1)
            onItemViewAttachedToWindow.invoke(holder)
        }
    }

    override fun onViewDetachedFromWindow(holder: DslViewHolder) {
        super.onViewDetachedFromWindow(holder)

        val dslAdapterItem = when {
            isAdapterStatus() -> dslAdapterStatusItem
            holder.adapterPosition in getValidFilterDataList().indices -> getAdapterItem(holder.adapterPosition)
            else -> null
        }

        dslAdapterItem?.apply {
            onItemViewDetachedToWindow.invoke(holder)
        }
    }

    //</editor-fold desc="生命周期方法">

    //<editor-fold desc="其他方法">

    /**
     * 只会执行一次
     * */
    var onDispatchUpdatesAfterOnce: ((dslAdapter: DslAdapter) -> Unit)? = null

    /**
     * [Diff]操作结束之后的通知事件
     * */
    override fun onDispatchUpdatesAfter(dslAdapter: DslAdapter) {
        onDispatchUpdatesAfterOnce?.invoke(dslAdapter)
        onDispatchUpdatesAfterOnce = null
    }

    //</editor-fold>

    //<editor-fold desc="辅助方法">

    /**
     * 适配器当前是情感图状态
     * */
    fun isAdapterStatus(): Boolean {
        return dslAdapterStatusItem.isInAdapterStatus()
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

    /**
     * 设置[Adapter]需要显示情感图的状态
     * [DslAdapterStatusItem.ADAPTER_STATUS_NONE]
     * [DslAdapterStatusItem.ADAPTER_STATUS_EMPTY]
     * [DslAdapterStatusItem.ADAPTER_STATUS_LOADING]
     * [DslAdapterStatusItem.ADAPTER_STATUS_ERROR]
     * */
    fun setAdapterStatus(status: Int) {
        if (dslAdapterStatusItem.itemState == status) {
            return
        }
        dslAdapterStatusItem.itemState = status
        notifyDataSetChanged()
        if (status == DslAdapterStatusItem.ADAPTER_STATUS_NONE) {
            updateItemDepend(defaultFilterParams ?: _defaultFilterParams())
        }
    }

    fun setLoadMoreEnable(enable: Boolean = true) {
        if (dslLoadMoreItem.itemEnableLoadMore == enable) {
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

    /**
     * [DslLoadMoreItem.ADAPTER_LOAD_NORMAL]
     * [DslLoadMoreItem.ADAPTER_LOAD_NO_MORE]
     * [DslLoadMoreItem.ADAPTER_LOAD_ERROR]
     * */
    fun setLoadMore(status: Int, notify: Boolean = true) {
        if (dslLoadMoreItem.itemEnableLoadMore && dslLoadMoreItem.itemState == status) {
            return
        }
        dslLoadMoreItem.itemState = status
        if (notify) {
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

    /**插入数据列表*/
    fun insertItem(index: Int, list: List<DslAdapterItem>) {
        if (list.isEmpty()) {
            return
        }
        dataItems.addAll(_validIndex(dataItems, index), list)
        _updateAdapterItems()
        updateItemDepend()
    }

    /**插入数据列表*/
    fun insertItem(index: Int, bean: DslAdapterItem) {
        dataItems.add(_validIndex(dataItems, index), bean)
        _updateAdapterItems()
        updateItemDepend()
    }

    /**移除一组数据*/
    fun removeItem(list: List<DslAdapterItem>) {
        val listInclude = mutableListOf<DslAdapterItem>()

        list.filterTo(listInclude) {
            dataItems.contains(it)
        }

        if (dataItems.removeAll(listInclude)) {
            _updateAdapterItems()
            updateItemDepend()
        }
    }

    /**移除数据*/
    fun removeItem(bean: DslAdapterItem) {
        if (dataItems.remove(bean)) {
            _updateAdapterItems()
            updateItemDepend()
        }
    }

    /**重置数据列表*/
    fun resetItem(list: List<DslAdapterItem>) {
        dataItems.clear()
        dataItems.addAll(list)
        _updateAdapterItems()
        updateItemDepend()
    }

    /**清理数据列表, 但不刷新界面*/
    fun clearItems() {
        dataItems.clear()
        _updateAdapterItems()
    }

    /**可以在回调中改变数据, 并且会自动刷新界面*/
    fun changeItems(
        filterParams: FilterParams = defaultFilterParams ?: _defaultFilterParams(),
        change: () -> Unit
    ) {
        change()
        _updateAdapterItems()
        updateItemDepend(filterParams)
    }

    fun changeDataItems(
        filterParams: FilterParams = defaultFilterParams ?: _defaultFilterParams(),
        change: (dataItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        changeItems(filterParams) {
            change(dataItems)
        }
    }

    fun changeHeaderItems(
        filterParams: FilterParams = defaultFilterParams ?: _defaultFilterParams(),
        change: (headerItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        changeItems(filterParams) {
            change(headerItems)
        }
    }

    fun changeFooterItems(
        filterParams: FilterParams = defaultFilterParams ?: _defaultFilterParams(),
        change: (footerItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        changeItems(filterParams) {
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

    /**创建默认的[FilterParams]*/
    fun _defaultFilterParams(): FilterParams {
        return FilterParams(
            just = dataItems.isEmpty(),
            async = getDataList().isNotEmpty(),
            justFilter = isAdapterStatus()
        )
    }

    /**调用[DiffUtil]更新界面*/
    fun updateItemDepend(
        filterParams: FilterParams = defaultFilterParams ?: _defaultFilterParams()
    ) {
        if (isAdapterStatus()) {
            //如果是情感图状态, 更新数据源没有意义
            return
        }

        if (adapterItems.isEmpty() && getValidFilterDataList().isEmpty()) {
            //都是空数据
            return
        }

        dslDataFilter?.let {
            it.updateFilterItemDepend(filterParams)

            if (filterParams == onceFilterParams) {
                onceFilterParams = null
            }
        }
    }

    /**获取有效过滤后的数据集合*/
    fun getValidFilterDataList(): List<DslAdapterItem> {
        return dslDataFilter?.filterDataList ?: adapterItems
    }

    /**更新界面上所有[DslAdapterItem]*/
    fun updateAllItem() {
        notifyItemRangeChanged(0, itemCount)
    }

    //</editor-fold desc="操作方法">

    //<editor-fold desc="操作符重载">

    /**
     * <pre>
     *  DslDemoItem()(){}
     * </pre>
     * */
    operator fun <T : DslAdapterItem> T.invoke(config: T.() -> Unit) {
        this.config()
        addLastItem(this)
    }

    /**
     * <pre>
     * this + DslAdapterItem()
     * </pre>
     * */
    operator fun <T : DslAdapterItem> plus(item: T): DslAdapter {
        addLastItem(item)
        return this
    }

    operator fun <T : DslAdapterItem> plus(list: List<T>): DslAdapter {
        addLastItem(list)
        return this
    }

    /**
     * <pre>
     * this - DslAdapterItem()
     * </pre>
     * */
    operator fun <T : DslAdapterItem> minus(item: T): DslAdapter {
        removeItem(item)
        return this
    }

    operator fun <T : DslAdapterItem> minus(list: List<T>): DslAdapter {
        removeItem(list)
        return this
    }

    //</editor-fold desc="操作符重载">

}