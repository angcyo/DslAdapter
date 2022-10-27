package com.angcyo.dsladapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.angcyo.dsladapter.annotation.UpdateByDiff
import com.angcyo.dsladapter.annotation.UpdateByNotify
import com.angcyo.dsladapter.annotation.UpdateFlag
import com.angcyo.dsladapter.filter.IFilterInterceptor


/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

//<editor-fold desc="Item操作">

/**
 * 通过条件, 查找[DslAdapterItem].
 *
 * @param useFilterList 是否使用过滤后的数据源. 通常界面上显示的是过滤后的数据, 所有add的数据源在非过滤列表中
 * */
fun DslAdapter.findItem(
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
): DslAdapterItem? {
    return getDataList(useFilterList).find(predicate)
}

fun <Item : DslAdapterItem> DslAdapter.findItem(
    itemClass: Class<Item>,
    useFilterList: Boolean = true
): Item? {
    return findItem(useFilterList) {
        it.className() == itemClass.className()
    } as? Item
}

inline fun <reified Item : DslAdapterItem> DslAdapter.find(
    tag: String? = null,
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean = {
        when {
            tag != null -> it.itemTag == tag
            it is Item -> true
            else -> false
        }
    }
): Item? {
    return getDataList(useFilterList).find(predicate) as? Item
}

inline fun <reified Item : DslAdapterItem> DslAdapter.findItem(
    tag: String? = null,
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean = {
        when {
            tag != null -> it.itemTag == tag
            it is Item -> true
            else -> false
        }
    },
    dsl: Item.() -> Unit
): Item? {
    return find<Item>(tag, useFilterList, predicate)?.apply(dsl)
}

/**更新第一个满足条件的item*/
@UpdateByNotify
fun DslAdapter.updateItem(
    payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART,
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
): DslAdapterItem? {
    return findItem(useFilterList, predicate)?.apply {
        updateAdapterItem(payload, useFilterList)
    }
}

/**更新所有满足条件的item*/
@UpdateByNotify
fun DslAdapter.updateAllItemBy(
    payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART,
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
): List<DslAdapterItem> {
    val result = mutableListOf<DslAdapterItem>()
    getDataList(useFilterList).forEach {
        if (predicate(it)) {
            result.add(it)
            it.updateAdapterItem(payload, useFilterList)
        }
    }
    return result
}

@UpdateByDiff
fun DslAdapter.removeItem(
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
) {
    val removeList = mutableListOf<DslAdapterItem>()
    getDataList(useFilterList).filterTo(removeList, predicate)
    if (removeList.isNotEmpty()) {
        render {
            removeItemFromAll(removeList)
        }
    }
}

/**通过[itemTags]更新对应的[DslAdapterItem]*/
@UpdateByNotify
fun DslAdapter.updateItem(vararg itemTags: String): List<DslAdapterItem> {
    return updateItem(DslAdapterItem.PAYLOAD_UPDATE_PART, true, *itemTags)
}

@UpdateByNotify
fun DslAdapter.updateItem(
    payload: Any?,
    useFilterList: Boolean,
    vararg itemTags: String
): List<DslAdapterItem> {
    val result = mutableListOf<DslAdapterItem>()
    itemTags.forEach { tag ->
        findItemByTag(tag, useFilterList)?.let { item ->
            result.add(item)
            if (isMain()) {
                item.updateAdapterItem(payload)
            } else {
                Handler(Looper.getMainLooper()).post { item.updateAdapterItem(payload) }
            }
        }
    }
    return result
}

fun DslAdapter.findItemByTag(
    tag: String?,
    useFilterList: Boolean = true
): DslAdapterItem? {
    if (tag == null) {
        return null
    }
    return findItem(useFilterList) {
        it.itemTag == tag
    }
}

fun <T : DslAdapterItem> DslAdapter.findItemByTag(
    tag: String?,
    clazz: Class<T>,
    useFilterList: Boolean = true
): T? {
    if (tag == null) {
        return null
    }
    val item = findItem(useFilterList) {
        it::class.java.isAssignableFrom(clazz) && it.itemTag == tag
    } ?: return null
    return item as T
}

fun DslAdapter.findItemByGroup(
    groups: List<String>,
    useFilterList: Boolean = true
): List<DslAdapterItem> {
    return getDataList(useFilterList).findItemByGroup(groups)
}

/**通过Tag查找item*/
fun List<DslAdapterItem>.findItemByTag(tag: String?): DslAdapterItem? {
    if (tag == null) {
        return null
    }
    return find {
        it.itemTag == tag
    }
}

/**通过group查找item*/
fun List<DslAdapterItem>.findItemByGroup(groups: List<String>): List<DslAdapterItem> {
    val result = mutableListOf<DslAdapterItem>()

    groups.forEach { group ->
        forEach {
            if (it.itemGroups.contains(group)) {
                result.add(it)
            }
        }
    }
    return result
}

/**返回[position]对应的item集合.[dataItems] [headerItems] [footerItems]*/
fun DslAdapter.getItemListByPosition(position: Int): List<DslAdapterItem>? {
    return when {
        position in headerItems.indices -> headerItems
        position - headerItems.size in dataItems.indices -> dataItems
        position - headerItems.size - dataItems.size in footerItems.indices -> footerItems
        else -> null
    }
}

fun DslAdapter.getItemListByItem(item: DslAdapterItem?): List<DslAdapterItem>? {
    return when {
        item == null -> null
        headerItems.contains(item) -> headerItems
        dataItems.contains(item) -> dataItems
        footerItems.contains(item) -> footerItems
        else -> null
    }
}

/**返回对应的集合, 和在集合中的索引*/
fun DslAdapter.getItemListPairByPosition(position: Int): Pair<MutableList<DslAdapterItem>?, Int> {
    val hSize = headerItems.size
    val dSize = dataItems.size
    return when {
        position in headerItems.indices -> headerItems to position
        position - hSize in dataItems.indices -> dataItems to (position - hSize)
        position - hSize - dSize in footerItems.indices -> footerItems to (position - hSize - dSize)
        else -> null to -1
    }
}

fun DslAdapter.getItemListPairByItem(item: DslAdapterItem?): Pair<MutableList<DslAdapterItem>?, Int> {
    return when {
        item == null -> null to -1
        headerItems.contains(item) -> headerItems to headerItems.indexOf(item)
        dataItems.contains(item) -> dataItems to dataItems.indexOf(item)
        footerItems.contains(item) -> footerItems to footerItems.indexOf(item)
        else -> null to -1
    }
}

/**通过[layoutId]快速添加一个[DslAdapterItem]*/
@UpdateFlag
fun DslAdapter.dslItem(
    @LayoutRes layoutId: Int,
    config: DslAdapterItem.() -> Unit = {}
): DslAdapterItem {
    val item = DslAdapterItem()
    item.itemLayoutId = layoutId
    addLastItem(item)
    item.config()
    return item
}

/**通过[layoutId]和直接绑定,快速添加一个[DslAdapterItem]*/
@UpdateFlag
fun DslAdapter.bindItem(@LayoutRes layoutId: Int, bindAction: ItemBindAction): DslAdapterItem {
    val item = DslAdapterItem()
    item.itemLayoutId = layoutId
    addLastItem(item)
    item.itemBindOverride = bindAction
    return item
}

@UpdateFlag
fun <T : DslAdapterItem> DslAdapter.dslItem(
    dslItem: T,
    config: T.() -> Unit = {}
): T {
    dslCustomItem(dslItem, config)
    return dslItem
}

@UpdateFlag
fun <T : DslAdapterItem> DslAdapter.dslCustomItem(
    dslItem: T,
    config: T.() -> Unit = {}
): T {
    addLastItem(dslItem)
    dslItem.config()
    return dslItem
}

/**空的占位item*/
@UpdateFlag
fun DslAdapter.renderEmptyItem(
    height: Int = 120 * dpi,
    backgroundColor: Int = Color.TRANSPARENT,
    list: MutableList<DslAdapterItem> = dataItems,
    action: DslAdapterItem.() -> Unit = {}
) {
    renderEmptyItem(height, ColorDrawable(backgroundColor), list, action)
}

@UpdateFlag
fun DslAdapter.renderEmptyItem(
    height: Int = 120 * dpi,
    background: Drawable?,
    list: MutableList<DslAdapterItem> = dataItems,
    action: DslAdapterItem.() -> Unit = {}
) {
    val adapterItem = DslAdapterItem()
    adapterItem.itemLayoutId = R.layout.base_empty_item
    adapterItem.itemBindOverride = { itemHolder, _, _, _ ->
        itemHolder.itemView.setBgDrawable(background)
        itemHolder.itemView.setWidthHeight(ViewGroup.LayoutParams.MATCH_PARENT, height)
    }
    adapterItem.action()
    addLastItem(list, adapterItem)
}

@UpdateFlag
fun DslAdapter.renderItem(count: Int = 1, init: DslAdapterItem.(index: Int) -> Unit) {
    for (i in 0 until count) {
        val adapterItem = DslAdapterItem()
        adapterItem.init(i)
        addLastItem(adapterItem)
    }
}

/**追加一个简单的[DslAdapterItem]*/
@UpdateFlag
fun DslAdapter.singleItem(
    @LayoutRes layoutId: Int,
    init: DslAdapterItem.() -> Unit = {},
    bind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem, payloads: List<Any>) -> Unit
) {
    val adapterItem = DslAdapterItem()
    adapterItem.itemLayoutId = layoutId
    adapterItem.itemBindOverride = bind
    adapterItem.init()
    addLastItem(adapterItem)
}

@UpdateFlag
fun <T> DslAdapter.renderItem(data: T, init: DslAdapterItem.() -> Unit) {
    val adapterItem = DslAdapterItem()
    adapterItem.itemData = data
    adapterItem.init()
    addLastItem(adapterItem)
}

/**获取所有指定类型的数据集合*/
inline fun <reified ItemData> DslAdapter.getAllItemData(useFilterList: Boolean = true): List<ItemData> {
    val result = mutableListOf<ItemData>()
    val itemList = getDataList(useFilterList)
    for (item in itemList) {
        if (item.itemData is ItemData) {
            result.add(item.itemData as ItemData)
        }
    }
    return result
}

/**枚举所有Item*/
fun DslAdapter.eachItem(
    useFilterList: Boolean = true,
    action: (index: Int, dslAdapterItem: DslAdapterItem) -> Unit
) {
    getDataList(useFilterList).forEachIndexed(action)
}

/**是否有item发生过改变*/
fun DslAdapter.haveItemChanged(useFilterList: Boolean = true): Boolean {
    for (item in getDataList(useFilterList)) {
        if (item.itemChanged) {
            return true
        }
    }
    return false
}

//</editor-fold desc="Item操作">

//<editor-fold desc="payload">

/**是否包含指定的[payload]*/
fun Iterable<*>.containsPayload(any: Any): Boolean {
    return havePayload {
        it == any
    }
}

fun Iterable<*>.havePayload(predicate: (Any?) -> Boolean): Boolean {
    var result = false
    for (payload in this) {
        result = if (payload is Iterable<*>) {
            payload.havePayload(predicate)
        } else {
            predicate(payload)
        }
        if (result) {
            break
        }
    }
    return result
}

/**是否要更新媒体, 比如:图片*/
fun Iterable<*>.isUpdateMedia(): Boolean {
    return count() <= 0 || containsPayload(DslAdapterItem.PAYLOAD_UPDATE_MEDIA)
}

/**是否局部更新*/
fun Iterable<*>.isUpdatePart(): Boolean {
    return containsPayload(DslAdapterItem.PAYLOAD_UPDATE_PART)
}

fun payload(vararg payload: Int): List<Int> {
    return if (payload.isEmpty()) {
        listOf(DslAdapterItem.PAYLOAD_UPDATE_PART)
    } else {
        payload.toList()
    }
}

/**需要更新媒体的负载*/
fun mediaPayload(): List<Int> =
    payload(DslAdapterItem.PAYLOAD_UPDATE_PART, DslAdapterItem.PAYLOAD_UPDATE_MEDIA)

//</editor-fold desc="payload">

//<editor-fold desc="AdapterStatus">

/**当前情感图的状态*/
fun DslAdapter.adapterStatus() =
    if (dslAdapterStatusItem.itemEnable && dslAdapterStatusItem.itemStateEnable) {
        dslAdapterStatusItem.itemState
    } else {
        null
    }

fun DslAdapter.isAdapterStatusLoading() =
    dslAdapterStatusItem.itemState == DslAdapterStatusItem.ADAPTER_STATUS_LOADING

fun DslAdapter.justRunFilterParams() = defaultFilterParams!!.apply {
    justRun = true
    asyncDiff = false
}

/**显示情感图[加载中]*/
@UpdateByDiff
fun DslAdapter.toLoading() {
    updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
}

@UpdateFlag
fun DslAdapter.loadingStatus() {
    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
}

/**显示情感图[空数据]*/
@UpdateByDiff
fun DslAdapter.toEmpty() {
    updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
}

@UpdateFlag
fun DslAdapter.emptyStatus() {
    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
}

/**显示情感图[错误]*/
@UpdateByDiff
fun DslAdapter.toError(error: Throwable? = null) {
    dslAdapterStatusItem.itemErrorThrowable = error
    updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_ERROR)
}

@UpdateFlag
fun DslAdapter.errorStatus(error: Throwable? = null) {
    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_ERROR, error)
}

/**显示情感图[正常]*/
@UpdateByDiff
fun DslAdapter.toNone() {
    updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
}

@UpdateFlag
fun DslAdapter.noneStatus() {
    setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
}

/**自动判断Adapter的当前状态*/
@UpdateByDiff
fun DslAdapter.updateAdapterState(error: Throwable? = null) {
    if (error == null) {
        if (adapterItems.isEmpty()) {
            toEmpty()
        } else {
            toNone()
        }
    } else {
        toError(error)
    }
}

//</editor-fold desc="AdapterStatus">

//<editor-fold desc="LoadMore">

/**加载更多失败*/
@UpdateByNotify
fun DslAdapter.toLoadMoreError(
    error: Throwable? = null,
    payload: Any? = null,
    notify: Boolean = true
) {
    dslLoadMoreItem.itemErrorThrowable = error
    setLoadMore(DslLoadMoreItem.LOAD_MORE_ERROR, payload, notify)
}

/**加载更多结束*/
@UpdateByNotify
fun DslAdapter.toLoadMoreEnd(payload: Any? = null, notify: Boolean = true) {
    setLoadMore(DslLoadMoreItem.LOAD_MORE_NORMAL, payload, notify)
}

/**无加载更多*/
@UpdateByNotify
fun DslAdapter.toLoadNoMore(payload: Any? = null, notify: Boolean = true) {
    setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE, payload, notify)
}

/**快速同时监听刷新/加载更多的回调*/
fun DslAdapter.onRefreshOrLoadMore(action: (itemHolder: DslViewHolder, loadMore: Boolean) -> Unit) {
    dslAdapterStatusItem.onRefresh = {
        action(it, false)
    }
    dslLoadMoreItem.onLoadMore = {
        action(it, true)
    }
}

//</editor-fold desc="LoadMore">

//<editor-fold desc="Update">

/**立即更新*/
@UpdateByDiff
fun DslAdapter.updateNow(filterParams: FilterParams = justRunFilterParams()) =
    updateItemDepend(filterParams)

/**延迟通知*/
@UpdateByDiff
fun DslAdapter.delayNotify(filterParams: FilterParams = FilterParams(notifyDiffDelay = 300)) {
    updateItemDepend(filterParams)
}

/**更新列表中所有的[DslAdapterItem]*/
@UpdateByNotify
fun List<DslAdapterItem>.updateAdapterItem(payload: Any? = DslAdapterItem.PAYLOAD_UPDATE_PART) {
    /**更新界面上所有[DslAdapterItem]*/
    forEach {
        it.diffResult(null, null)
        it.updateAdapterItem(payload)
    }
}

//</editor-fold desc="Update">

//<editor-fold desc="操作扩展">

/**查找符合条件的item集合*/
fun DslAdapter.findItemList(
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
): List<DslAdapterItem> {
    val list = getDataList(useFilterList)
    val result = mutableListOf<DslAdapterItem>()

    for (it in list) {
        if (predicate(it)) {
            result.add(it)
        }
    }

    return result
}

/**查找相同类名的item
 * [continuous]连续or不连续*/
fun <T : DslAdapterItem> DslAdapter.findSameClassItem(
    item: T,
    useFilterList: Boolean = true,
    continuous: Boolean = true //需要连续的item
): List<T> {
    val list = getDataList(useFilterList)
    val result = mutableListOf<T>()

    if (continuous) {
        var findAnchor = false /*是否找到锚点*/
        for (it in list) {
            if (it.className() == item.className()) {
                result.add(it as T)
            } else {
                if (findAnchor) {
                    break
                } else {
                    result.clear()
                }
            }
            findAnchor = findAnchor || it == item
        }
    } else {
        for (it in list) {
            if (it.className() == item.className()) {
                result.add(it as T)
            }
        }
    }

    return result
}

/**更新[itemIsSelected]属性*/
@UpdateByNotify
fun DslAdapter.selectItem(
    selected: Boolean = true,
    update: Boolean = true,
    useFilterList: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
) {
    for (item in getDataList(useFilterList)) {
        if (predicate(item)) {
            if (item.itemIsSelected != selected) {
                item.itemIsSelected = selected
                if (update) {
                    item.updateAdapterItem(useFilterList = useFilterList)
                }
            }
        }
    }
}

//</editor-fold desc="操作扩展">

//<editor-fold desc="Interceptor">

/**添加一个前置过滤器*/
fun DslAdapter.addBeforeInterceptor(interceptor: IFilterInterceptor): IFilterInterceptor {
    dslDataFilter?.beforeFilterInterceptorList?.add(interceptor)
    return interceptor
}

fun DslAdapter.removeBeforeInterceptor(interceptor: IFilterInterceptor): Boolean {
    return dslDataFilter?.beforeFilterInterceptorList?.remove(interceptor) ?: false
}

/**添加一个后置过滤器*/
fun DslAdapter.addAfterInterceptor(interceptor: IFilterInterceptor): IFilterInterceptor {
    dslDataFilter?.afterFilterInterceptorList?.add(interceptor)
    return interceptor
}

fun DslAdapter.removeAfterInterceptor(interceptor: IFilterInterceptor): Boolean {
    return dslDataFilter?.afterFilterInterceptorList?.remove(interceptor) ?: false
}

fun IFilterInterceptor.addToAfter(adapter: DslAdapter, update: Boolean = true) {
    adapter.addAfterInterceptor(this)
    if (update) {
        adapter.updateItemDepend()
    }
}

fun IFilterInterceptor.removeFromAfter(adapter: DslAdapter, update: Boolean = true) {
    adapter.removeAfterInterceptor(this)
    if (update) {
        adapter.updateItemDepend()
    }
}

//</editor-fold desc="Interceptor">


