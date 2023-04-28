package com.angcyo.dsladapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.annotation.UpdateByDiff
import com.angcyo.dsladapter.annotation.UpdateByNotify
import com.angcyo.dsladapter.annotation.UpdateFlag
import com.angcyo.dsladapter.filter.IFilterInterceptor
import com.angcyo.dsladapter.internal.AdapterStatusFilterInterceptor
import com.angcyo.dsladapter.internal.AnimateDelayHandler
import com.angcyo.dsladapter.internal.LoadMoreFilterInterceptor
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * https://github.com/angcyo/DslAdapter
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class DslAdapter(dataItems: List<DslAdapterItem>? = null) :
    RecyclerView.Adapter<DslViewHolder>(), OnDispatchUpdatesListener {

    /**
     * 为了简单起见, 这里写死套路, 理论上应该用状态器管理的.
     * 2.0.0 版本更新之后, [dslAdapterStatusItem] [dslLoadMoreItem] 将在过滤数据源后加载追加,
     * 所以不能在[adapterItems]中找到.
     * @since 2.0.0
     * */
    var dslAdapterStatusItem = DslAdapterStatusItem()
    var dslLoadMoreItem = DslLoadMoreItem()

    /**[dslAdapterStatusItem] [dslLoadMoreItem] 功能的支持*/
    var adapterStatusIFilterInterceptor: IFilterInterceptor = AdapterStatusFilterInterceptor()
    var loadMoreIFilterInterceptor: IFilterInterceptor = LoadMoreFilterInterceptor()

    /**延迟计算器*/
    var adapterItemAnimateDelayHandler: AnimateDelayHandler = AnimateDelayHandler()

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

    /**单/多选助手*/
    val itemSelectorHelper = ItemSelectorHelper(this)

    /**
     * 一次性的[FilterParams], 使用完之后会被置空,调用无参[updateItemDepend]方法时使用.
     * */
    var onceFilterParams: FilterParams? = null

    /**默认的[FilterParams]*/
    @NonNull
    var defaultFilterParams: FilterParams? = null
        get() {
            return onceFilterParams ?: (field ?: _defaultFilterParams())
        }

    /**
     * [Diff]更新数据后回调, 只会执行一次
     * */
    var onDispatchUpdatesAfterOnce: DispatchUpdates? = null

    val dispatchUpdatesBeforeList = mutableListOf<DispatchUpdates>()
    val dispatchUpdatesAfterList = mutableListOf<DispatchUpdates>()
    val dispatchUpdatesAfterOnceList = mutableListOf<DispatchUpdates>()

    val itemBindObserver = mutableSetOf<ItemBindAction>()

    val itemUpdateDependObserver = mutableSetOf<ItemUpdateDependAction>()

    //关联item type和item layout id
    val _itemLayoutHold = hashMapOf<Int, Int>()

    init {
        updateDataFilter(DslDataFilter(this))
        dataItems?.let {
            this.dataItems.clear()
            this.dataItems.addAll(dataItems)
            _updateAdapterItems()
            updateItemDepend(FilterParams(asyncDiff = false, justRun = true))
        }
    }

    //<editor-fold desc="生命周期方法">

    override fun getItemViewType(position: Int): Int {
        var type = 0
        getItemData(position)?.apply {
            type = itemViewType ?: itemLayoutId
            _itemLayoutHold[type] = itemLayoutId
        }
        return type
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DslViewHolder {
        val layoutId = _itemLayoutHold[viewType] ?: 0
        if (layoutId <= 0) {
            throw IllegalStateException("请检查是否未指定[itemLayoutId]")
        }
        //viewType, 就是布局的 Id, 这是设计核心原则.
        val dslViewHolder: DslViewHolder
        val itemView: View = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        dslViewHolder = DslViewHolder(itemView)
        return dslViewHolder
    }

    override fun getItemCount(): Int {
        return getValidFilterDataList().size
    }

    override fun onBindViewHolder(
        holder: DslViewHolder,
        position: Int,
        payloads: List<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)

        //核心, 开始绑定界面
        val dslItem = getItemData(position)
        dslItem?.let {
            dslItem.itemDslAdapter = this
            dslItem.itemBind(holder, position, dslItem, payloads)
            holder.isBindView = true

            //绑定监听
            itemBindObserver.forEach {
                it(holder, position, dslItem, payloads)
            }
        }
    }

    override fun onBindViewHolder(holder: DslViewHolder, position: Int) {
        //no op
    }

    var _recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        _recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        _recyclerView = null
    }

    override fun onViewAttachedToWindow(holder: DslViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.getDslAdapterItem()?.apply {
            holder.itemView.fullSpan(itemSpanCount == DslAdapterItem.FULL_ITEM)
            itemViewAttachedToWindow.invoke(holder, holder.adapterPosition)
        }
    }

    override fun onViewDetachedFromWindow(holder: DslViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.getDslAdapterItem()?.apply {
            itemViewDetachedToWindow.invoke(holder, holder.adapterPosition)
        }
    }

    override fun onViewRecycled(holder: DslViewHolder) {
        super.onViewRecycled(holder)
        holder.getDslAdapterItem()?.apply {
            itemViewRecycled.invoke(holder, holder.adapterPosition)
        }
    }

    override fun onFailedToRecycleView(holder: DslViewHolder): Boolean {
        return super.onFailedToRecycleView(holder).apply {
            L.w("是否回收失败:$holder $this")
        }
    }

    /**返回[DslViewHolder]对应的[DslAdapterItem]*/
    fun DslViewHolder.getDslAdapterItem(): DslAdapterItem? {
        return when (adapterPosition) {
            in getValidFilterDataList().indices -> getItemData(adapterPosition)
            else -> null
        }
    }

    //</editor-fold desc="生命周期方法">

    //<editor-fold desc="其他方法">

    override fun onDispatchUpdatesBefore(dslAdapter: DslAdapter) {
        dispatchUpdatesBeforeList.forEach {
            it.invoke(dslAdapter)
        }
    }

    /**
     * [Diff]操作结束之后的通知事件
     * */
    override fun onDispatchUpdatesAfter(dslAdapter: DslAdapter) {
        onDispatchUpdatesAfterOnce?.invoke(dslAdapter)
        onDispatchUpdatesAfterOnce = null

        dispatchUpdatesAfterOnceList.forEach {
            it.invoke(dslAdapter)
        }

        dispatchUpdatesAfterOnceList.clear()

        dispatchUpdatesAfterList.forEach {
            it.invoke(dslAdapter)
        }
    }

    fun onDispatchUpdates(action: DispatchUpdates) {
        dispatchUpdatesAfterList.add(action)
    }

    fun onDispatchUpdatesAfter(action: DispatchUpdates) {
        dispatchUpdatesAfterList.add(action)
    }

    fun onDispatchUpdatesBefore(action: DispatchUpdates) {
        dispatchUpdatesBeforeList.add(action)
    }

    fun onDispatchUpdatesOnce(action: DispatchUpdates) {
        dispatchUpdatesAfterOnceList.add(action)
    }

    /**观察[onBindViewHolder]*/
    fun observeItemBind(itemBindAction: ItemBindAction) {
        itemBindObserver.add(itemBindAction)
    }

    fun removeItemBind(itemBindAction: ItemBindAction) {
        itemBindObserver.remove(itemBindAction)
    }

    /**观察[updateItemDepend]*/
    fun observeItemUpdateDepend(action: ItemUpdateDependAction) {
        itemUpdateDependObserver.add(action)
    }

    fun removeItemUpdateDepend(action: ItemUpdateDependAction) {
        itemUpdateDependObserver.remove(action)
    }

    /**更新数据过滤器[DslDataFilter]*/
    fun updateDataFilter(dataFilter: DslDataFilter?) {
        if (dslDataFilter == dataFilter) {
            return
        }
        //remove
        dslDataFilter?.apply {
            removeDispatchUpdatesListener(this@DslAdapter)
            beforeFilterInterceptorList.remove(adapterStatusIFilterInterceptor)
            afterFilterInterceptorList.remove(loadMoreIFilterInterceptor)
        }
        dslDataFilter = dataFilter
        //add
        dslDataFilter?.apply {
            addDispatchUpdatesListener(this@DslAdapter)
            beforeFilterInterceptorList.add(0, adapterStatusIFilterInterceptor)
            afterFilterInterceptorList.add(loadMoreIFilterInterceptor)
        }
    }

    //</editor-fold>

    //<editor-fold desc="辅助方法">

    @UpdateFlag
    fun _updateAdapterItems() {
        //整理数据
        adapterItems.clear()
        adapterItems.addAll(headerItems)
        adapterItems.addAll(dataItems)
        adapterItems.addAll(footerItems)

        adapterItems.forEach { item ->
            //提前赋值
            item.itemDslAdapter = this
            item.clearItemGroupParamsCache()
        }
    }

    /**使得所有item, 都进入待更新的状态*/
    @UpdateFlag
    fun changingAllItem() {
        adapterItems.forEach { item ->
            item.itemChanging = true
        }
    }

    //</editor-fold desc="辅助方法">

    //<editor-fold desc="操作方法">

    /**
     * 适配器当前是情感图状态
     * */
    fun isAdapterStatus(): Boolean {
        return dslAdapterStatusItem.isInStateLayout()
    }

    /**
     * 设置[Adapter]需要显示情感图的状态, 并不会触发diff只是状态的设置
     * [DslAdapterStatusItem.ADAPTER_STATUS_NONE]
     * [DslAdapterStatusItem.ADAPTER_STATUS_EMPTY]
     * [DslAdapterStatusItem.ADAPTER_STATUS_LOADING]
     * [DslAdapterStatusItem.ADAPTER_STATUS_ERROR]
     * */
    @UpdateFlag
    fun setAdapterStatus(status: Int, error: Throwable? = null) {
        if (dslAdapterStatusItem.itemState == status) {
            return
        }
        if (status == DslAdapterStatusItem.ADAPTER_STATUS_ERROR) {
            dslAdapterStatusItem.itemErrorThrowable = error
        }
        dslAdapterStatusItem.itemState = status
        dslAdapterStatusItem.itemUpdateFlag = true
    }

    /**设置[Adapter]需要显示情感图的状态, 并触发Diff更新界面
     * 注意: 此方法会触发Diff
     * */
    @UpdateByDiff
    fun updateAdapterStatus(status: Int) {
        if (dslAdapterStatusItem.itemState == status) {
            return
        }
        dslAdapterStatusItem.itemDslAdapter = this
        dslAdapterStatusItem.itemState = status
        dslAdapterStatusItem.itemChanging = true
    }

    /**自动设置状态
     * 根据[adapterItems]的数量, 智能切换[AdapterState]*/
    @UpdateFlag
    fun autoAdapterStatus(error: Throwable? = null) {
        if (isAdapterStatus()) {
            if (error == null) {
                val emptyCount = 0
                if (adapterItems.size <= emptyCount) {
                    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
                } else {
                    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                }
            } else {
                dslAdapterStatusItem.itemErrorThrowable = error
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_ERROR)
            }
        } else {
            if (error != null) {
                dslAdapterStatusItem.itemErrorThrowable = error
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_ERROR)
            }
        }
    }

    @UpdateFlag
    fun setAdapterStatusEnable(enable: Boolean = true) {
        val old = dslAdapterStatusItem.itemStateEnable
        if (old == enable) {
            return
        }
        dslAdapterStatusItem.itemStateEnable = enable
    }

    @UpdateFlag
    fun setLoadMoreEnable(enable: Boolean = true) {
        if (dslLoadMoreItem.itemStateEnable == enable) {
            return
        }
        dslLoadMoreItem.itemStateEnable = enable
    }

    /**
     * [DslLoadMoreItem.LOAD_MORE_NORMAL]
     * [DslLoadMoreItem.LOAD_MORE_NO_MORE]
     * [DslLoadMoreItem.LOAD_MORE_ERROR]
     * */
    @UpdateByNotify
    fun setLoadMore(status: Int, payload: Any? = null, notify: Boolean = true) {
        if (dslLoadMoreItem.itemStateEnable && dslLoadMoreItem.itemState == status) {
            return
        }
        dslLoadMoreItem.itemDslAdapter = this
        dslLoadMoreItem.itemState = status
        if (notify) {
            notifyItemChanged(dslLoadMoreItem, payload)
        }
    }

    //<editor-fold desc="Item操作">

    /**
     * 在最后的位置插入数据
     */
    @UpdateFlag
    fun addLastItem(item: DslAdapterItem) {
        insertItem(-1, item)
    }

    @UpdateFlag
    fun <T : DslAdapterItem> addLastItem(item: T, init: T.() -> Unit) {
        insertItem(-1, item, init)
    }

    /**支持指定数据源[list]*/
    @UpdateFlag
    fun <T : DslAdapterItem> addLastItem(
        list: MutableList<DslAdapterItem>,
        item: T,
        init: T.() -> Unit = {}
    ) {
        insertItem(list, -1, item, init)
    }

    @UpdateFlag
    fun addLastItem(item: List<DslAdapterItem>) {
        insertItem(-1, item)
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
    @UpdateFlag
    fun insertItem(index: Int, list: List<DslAdapterItem>) {
        if (list.isEmpty()) {
            return
        }
        dataItems.addAll(_validIndex(dataItems, index), list)
        _updateAdapterItems()
    }

    /**插入数据列表*/
    @UpdateFlag
    fun insertItem(index: Int, item: DslAdapterItem, checkExist: Boolean = true) {
        if (checkExist && dataItems.contains(item)) {
            return
        }
        dataItems.add(_validIndex(dataItems, index), item)
        _updateAdapterItems()
    }

    @UpdateFlag
    fun insertItem(item: DslAdapterItem, index: Int = -1, checkExist: Boolean = true) {
        insertItem(index, item, checkExist)
    }

    @UpdateFlag
    fun insertHeaderItem(item: DslAdapterItem, index: Int = -1, checkExist: Boolean = true) {
        if (checkExist && headerItems.contains(item)) {
            return
        }
        headerItems.add(_validIndex(headerItems, index), item)
        _updateAdapterItems()
    }

    @UpdateFlag
    fun insertFooterItem(item: DslAdapterItem, index: Int = -1, checkExist: Boolean = true) {
        if (checkExist && footerItems.contains(item)) {
            return
        }
        footerItems.add(_validIndex(footerItems, index), item)
        _updateAdapterItems()
    }

    /**先插入数据, 再初始化*/
    @UpdateFlag
    fun <T : DslAdapterItem> insertItem(index: Int, item: T, init: T.() -> Unit = {}) {
        dataItems.add(_validIndex(dataItems, index), item)
        _updateAdapterItems()
        item.init()
    }

    /**支持指定数据源[list]*/
    @UpdateFlag
    fun <T : DslAdapterItem> insertItem(
        list: MutableList<DslAdapterItem>,
        index: Int,
        item: T,
        init: T.() -> Unit
    ) {
        list.add(_validIndex(list, index), item)
        _updateAdapterItems()
        item.init()
    }

    /**移除一组数据*/
    @UpdateFlag
    fun removeItem(list: List<DslAdapterItem>) {
        val listInclude = mutableListOf<DslAdapterItem>()

        list.filterTo(listInclude) {
            dataItems.contains(it)
        }

        if (dataItems.removeAll(listInclude)) {
            listInclude.forEach { it.itemRemoveFlag = true }
            _updateAdapterItems()
        }
    }

    @UpdateFlag
    fun removeHeaderItem(list: List<DslAdapterItem>) {
        val listInclude = mutableListOf<DslAdapterItem>()

        list.filterTo(listInclude) {
            headerItems.contains(it)
        }

        if (headerItems.removeAll(listInclude)) {
            listInclude.forEach { it.itemRemoveFlag = true }
            _updateAdapterItems()
        }
    }

    @UpdateFlag
    fun removeFooterItem(list: List<DslAdapterItem>) {
        val listInclude = mutableListOf<DslAdapterItem>()

        list.filterTo(listInclude) {
            footerItems.contains(it)
        }

        if (footerItems.removeAll(listInclude)) {
            listInclude.forEach { it.itemRemoveFlag = true }
            _updateAdapterItems()
        }
    }

    @UpdateFlag
    fun removeItemFromAll(item: DslAdapterItem, updateOther: Boolean = true) {
        removeItemFrom(dataItems, item, updateOther)
        removeItemFrom(headerItems, item, updateOther)
        removeItemFrom(footerItems, item, updateOther)
    }

    @UpdateFlag
    fun removeItemFromAll(list: List<DslAdapterItem>) {
        removeItem(list)
        removeHeaderItem(list)
        removeFooterItem(list)
    }

    /**移除数据*/
    @UpdateFlag
    fun removeItem(item: DslAdapterItem, updateOther: Boolean = true) {
        removeItemFrom(dataItems, item, updateOther)
    }

    /**移除数据*/
    @UpdateFlag
    fun removeHeaderItem(item: DslAdapterItem, updateOther: Boolean = true) {
        removeItemFrom(headerItems, item, updateOther)
    }

    @UpdateFlag
    fun removeFooterItem(item: DslAdapterItem, updateOther: Boolean = true) {
        removeItemFrom(footerItems, item, updateOther)
    }

    @UpdateFlag
    fun removeItemFrom(
        fromList: MutableList<DslAdapterItem>,
        item: DslAdapterItem,
        updateOther: Boolean
    ) {
        val index = adapterItems.indexOf(item)
        if (index != -1) {
            if (fromList.remove(item)) {
                item.itemRemoveFlag = true
                if (updateOther) {
                    for (i in (index + 1) until adapterItems.size) {
                        //更新之后的item
                        adapterItems.getOrNull(i)?.itemUpdateFlag = true
                    }
                }
                _updateAdapterItems()
            }
        }
    }

    /**重置数据列表*/
    @UpdateFlag
    fun resetItem(list: List<DslAdapterItem>) {
        dataItems.clear()
        dataItems.addAll(list)
        _updateAdapterItems()
    }

    /**清理数据列表, 但不刷新界面*/
    @UpdateFlag
    fun clearItems() {
        dataItems.clear()
        _updateAdapterItems()
    }

    @UpdateFlag
    fun clearHeaderItems() {
        headerItems.clear()
        _updateAdapterItems()
    }

    @UpdateFlag
    fun clearFooterItems() {
        footerItems.clear()
        _updateAdapterItems()
    }

    @UpdateFlag
    fun clearAllItems() {
        headerItems.clear()
        dataItems.clear()
        footerItems.clear()
        _updateAdapterItems()
    }

    /**可以在回调中改变数据, 并且会自动刷新界面*/
    @UpdateByDiff
    fun changeItems(
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        change: () -> Unit
    ) {
        render(updateState, filterParams) {
            change()
            _updateAdapterItems()
        }
    }

    @UpdateByDiff
    fun changeDataItems(
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        change: (dataItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        changeItems(updateState, filterParams) {
            change(dataItems)
        }
    }

    @UpdateByDiff
    fun changeHeaderItems(
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        change: (headerItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        changeItems(updateState, filterParams) {
            change(headerItems)
        }
    }

    @UpdateByDiff
    fun changeFooterItems(
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        change: (footerItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        changeItems(updateState, filterParams) {
            change(footerItems)
        }
    }

    @UpdateByDiff
    fun renderHeader(
        reset: Boolean = false,
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        render: DslAdapter.(headerItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        val delegateAdapter = DslAdapter()
        delegateAdapter.dslDataFilter = null
        delegateAdapter.render(headerItems)
        changeItems(updateState, filterParams) {
            if (reset) {
                headerItems.clear()
            }
            headerItems.addAll(delegateAdapter.adapterItems)
        }
    }

    @UpdateByDiff
    fun renderData(
        reset: Boolean = false,
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        render: DslAdapter.(dataItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        val delegateAdapter = DslAdapter()
        delegateAdapter.dslDataFilter = null
        delegateAdapter.render(dataItems)
        changeItems(updateState, filterParams) {
            if (reset) {
                dataItems.clear()
            }
            dataItems.addAll(delegateAdapter.adapterItems)
        }
    }

    @UpdateByDiff
    fun renderFooter(
        reset: Boolean = false,
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        render: DslAdapter.(footerItems: MutableList<DslAdapterItem>) -> Unit
    ) {
        val delegateAdapter = DslAdapter()
        delegateAdapter.dslDataFilter = null
        delegateAdapter.render(footerItems)
        changeItems(updateState, filterParams) {
            if (reset) {
                footerItems.clear()
            }
            footerItems.addAll(delegateAdapter.adapterItems)
        }
    }

    //</editor-fold desc="Item操作">

    /** 获取[fromItem]更新时, 有多少子项需要更新 */
    fun getUpdateDependItemListFrom(fromItem: DslAdapterItem): List<DslAdapterItem> {
        val notifyChildFormItemList = mutableListOf<DslAdapterItem>()
        getValidFilterDataList().forEachIndexed { index, dslAdapterItem ->
            if (fromItem.isItemInUpdateList(dslAdapterItem, index)) {
                notifyChildFormItemList.add(dslAdapterItem)
            }
        }
        return notifyChildFormItemList
    }

    /**获取有效过滤后的数据集合*/
    fun getValidFilterDataList(): List<DslAdapterItem> {
        return dslDataFilter?.filterDataList ?: adapterItems
    }

    fun getItemData(position: Int, useFilterList: Boolean = true): DslAdapterItem? {
        val list = getDataList(useFilterList)
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

    /**包含[com.angcyo.dsladapter.DslAdapterItem.itemSubList]*/
    fun getDataAndSubList(useFilterList: Boolean = false): List<DslAdapterItem> {
        val list = if (useFilterList) getValidFilterDataList() else adapterItems
        val result = mutableListOf<DslAdapterItem>()
        fun addSubList(item: DslAdapterItem) {
            result.add(item)
            item.itemSubList.forEach {
                addSubList(it)
            }
        }
        list.forEach {
            addSubList(it)
        }
        return result
    }

    /**创建默认的[FilterParams]*/
    fun _defaultFilterParams(): FilterParams {
        return FilterParams()
    }

    /**渲染[DslAdapter]中的item*/
    @UpdateByDiff
    fun render(
        updateState: Boolean = true,
        filterParams: FilterParams = defaultFilterParams!!,
        action: DslAdapter.() -> Unit
    ) {
        //之前的状态
        val oldState = adapterStatus()
        action()
        _updateAdapterItems()
        if (updateState) {
            val nowState = adapterStatus()
            if (adapterItems.isEmpty()) {
                if ((oldState == null || oldState <= 0) &&
                    (nowState == DslAdapterStatusItem.ADAPTER_STATUS_LOADING || nowState == DslAdapterStatusItem.ADAPTER_STATUS_ERROR)
                ) {
                    //未被初始化, 并且新状态新 加载中/错误... 则保持此状态
                } else {
                    emptyStatus()
                }
            } else {
                noneStatus()
            }
        }
        updateItemDepend(filterParams)
    }

    /**调用[DiffUtil]更新界面*/
    @UpdateByDiff
    fun updateItemDepend(filterParams: FilterParams = defaultFilterParams!!) {

        itemUpdateDependObserver.forEach {
            it(filterParams)
        }

        dslDataFilter?.let {
            it.updateFilterItemDepend(filterParams)

            if (filterParams == onceFilterParams) {
                onceFilterParams = null
            }
        }
    }

    /**刷新某一个item, 支持过滤数据源*/
    @UpdateByNotify
    fun notifyItemChanged(
        item: DslAdapterItem?,
        payload: Any? = null,
        useFilterList: Boolean = true
    ) {
        if (item == null) {
            return
        }
        val list = getDataList(useFilterList)
        val indexOf = list.indexOf(item)

        if (indexOf in list.indices) {
            item.diffResult(null, null)
            notifyItemChangedPayload(indexOf, payload)
        }
    }

    /**[notifyDataSetChanged]*/
    @UpdateByNotify
    fun notifyDataChanged() {
        _updateAdapterItems()
        dslDataFilter?.clearTask()
        dslDataFilter?.filterDataList?.clear()
        dslDataFilter?.filterDataList?.addAll(adapterItems)
        adapterItems.forEach {
            it.diffResult(null, null)
        }
        notifyDataSetChanged()
    }

    /**更新界面上所有[DslAdapterItem]*/
    @UpdateByNotify
    fun updateAllItem(payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART) {
        adapterItems.forEach {
            it.diffResult(null, null)
        }
        notifyItemRangeChanged(0, itemCount, payload)
    }

    /**更新界面上所有[DslAdapterItem]*/
    @UpdateByNotify
    fun updateAllDataItem(payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART) {
        dataItems.forEach {
            it.diffResult(null, null)
        }
        notifyItemRangeChanged(0, dataItems.size, payload)
    }


    /**更新界面上所有[DslAdapterItem]*/
    @UpdateByNotify
    fun updateAllHeaderItem(payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART) {
        headerItems.forEach {
            it.diffResult(null, null)
        }
        notifyItemRangeChanged(dataItems.size, headerItems.size, payload)
    }

    /**更新界面上所有[DslAdapterItem]*/
    @UpdateByNotify
    fun updateAllFooterItem(payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART) {
        footerItems.forEach {
            it.diffResult(null, null)
        }
        notifyItemRangeChanged(dataItems.size + headerItems.size, footerItems.size, payload)
    }

    /**更新一批[DslAdapterItem]*/
    @UpdateByNotify
    fun updateItems(
        list: Iterable<DslAdapterItem>,
        payload: Any? = null,
        useFilterList: Boolean = true
    ) {
        getDataList(useFilterList).apply {
            for (item in list) {
                val indexOf = indexOf(item)

                if (indexOf in this.indices) {
                    item.diffResult(null, null)
                    notifyItemChangedPayload(indexOf, payload)
                }
            }
        }
    }

    @UpdateByNotify
    fun notifyItemChangedPayload(position: Int, payloads: Any? = null) {
//        if (payloads is Iterable<*>) {
//            for (payload in payloads) {
//                //为了避免多次触发[onChanged]事件, 取消此方式
//                notifyItemChanged(position, payload)
//            }
//        } else {
//            notifyItemChanged(position, payloads)
//        }
        notifyItemChanged(position, payloads)
    }

    //</editor-fold desc="操作方法">

    //<editor-fold desc="操作符重载">

    /**
     * <pre>
     *  DslDemoItem()(){}
     * </pre>
     * */
    @UpdateFlag
    operator fun <T : DslAdapterItem> T.invoke(config: T.() -> Unit = {}) {
        addLastItem(this, config)
    }

    /**
     * ```
     *  DslDemoItem()(0){}
     * ```
     * */
    @UpdateFlag
    operator fun <T : DslAdapterItem> T.invoke(index: Int, config: T.() -> Unit = {}) {
        insertItem(index, this, config)
    }

    @UpdateFlag
    operator fun <T : DslAdapterItem> T.invoke(
        list: MutableList<DslAdapterItem>,
        config: T.() -> Unit = {}
    ) {
        addLastItem(list, this, config)
    }

    @UpdateFlag
    operator fun <T : DslAdapterItem> T.invoke(
        index: Int,
        list: MutableList<DslAdapterItem>,
        config: T.() -> Unit = {}
    ) {
        insertItem(list, index, this, config)
    }

    /**
     * <pre>
     * this + DslAdapterItem()
     * </pre>
     * */
    @UpdateFlag
    operator fun <T : DslAdapterItem> plus(item: T): DslAdapter {
        addLastItem(item)
        return this
    }

    @UpdateFlag
    operator fun <T : DslAdapterItem> plus(list: List<T>): DslAdapter {
        addLastItem(list)
        return this
    }

    /**
     * <pre>
     * this - DslAdapterItem()
     * </pre>
     * */
    @UpdateFlag
    operator fun <T : DslAdapterItem> minus(item: T): DslAdapter {
        removeItemFromAll(item)
        return this
    }

    @UpdateFlag
    operator fun <T : DslAdapterItem> minus(list: List<T>): DslAdapter {
        removeItemFromAll(list)
        return this
    }

    /**
     * ```
     * this[1]
     * this[index]
     * this[index, false]
     *
     * 负数表示倒数
     * ```
     * */
    operator fun get(
        index: Int,
        useFilterList: Boolean = true,
        reverse: Boolean = true //是否开启反序, 倒数
    ): DslAdapterItem? {
        return getDataList(useFilterList).run {
            if (index >= 0 || !reverse)
                getOrNull(index)
            else
                getOrNull(size + index)
        }
    }

    /**
     * ```
     * this["tag"]
     * this["tag", false]
     * ```
     * */
    operator fun get(tag: String?, useFilterList: Boolean = true): DslAdapterItem? {
        return tag?.run { findItemByTag(tag, useFilterList) }
    }

    /**
     * ```
     * this[{ true }]
     * this[{ true }, false]
     * ```
     * */
    operator fun get(
        predicate: (DslAdapterItem) -> Boolean,
        useFilterList: Boolean = true
    ): List<DslAdapterItem> {
        return getDataList(useFilterList).filter(predicate)
    }

    /**
     * ```
     * this[DslAdapterItem::class.java]
     * ```
     * */
    operator fun <Item : DslAdapterItem> get(
        itemClass: Class<Item>,
        useFilterList: Boolean = true
    ): List<Item> {
        return getDataList(useFilterList).filter {
            it.className() == itemClass.className()
        } as List<Item>
    }

    /**
     * ```
     * this[DslAdapterItem::class]
     * ```
     * */
    operator fun <Item : DslAdapterItem> get(
        itemClass: KClass<Item>,
        useFilterList: Boolean = true
    ): List<Item> {
        return getDataList(useFilterList).filter {
            it.className() == itemClass.java.className()
        } as List<Item>
    }

    //</editor-fold desc="操作符重载">
}