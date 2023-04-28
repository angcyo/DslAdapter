package com.angcyo.dsladapter.data

import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.annotation.UpdateByDiff
import com.angcyo.dsladapter.annotation.UpdateFlag
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KClass

/**
 *数据更新Dsl配置项
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

class UpdateDataConfig {
    /**从第几页开始更新*/
    var startPage: Int = Page.FIRST_PAGE_INDEX

    /**需要加载的页码, 会偏移到指定位置*/
    var updatePage: Int = Page.FIRST_PAGE_INDEX

    /**页面数量*/
    var pageSize: Int = Page.PAGE_SIZE

    /**需要加载的数据*/
    var updateDataList: List<Any>? = null

    /**是否一直激活加载更多, 不管第一页数据不够*/
    var alwaysEnableLoadMore: Boolean = false

    var filterParams: FilterParams = FilterParams().apply {
        payload = listOf(
            DslAdapterItem.PAYLOAD_UPDATE_PART,
            DslAdapterItem.PAYLOAD_UPDATE_MEDIA
        )
    }

    /**本次更新, 需要更新的数据量*/
    var updateSize: () -> Int = {
        val listSize = if (updateDataList.isListEmpty()) 0 else (updateDataList?.size ?: 0)
        val pageSize = pageSize
        if (listSize > pageSize) {
            listSize
        } else {
            min(pageSize, listSize)
        }
    }

    /**
     * 更新已有的item, 创建不存在的item, 移除不需要的item
     * [oldItem] 如果有值, 则希望更新[oldItem]
     * [data] 需要更新的数据
     * [index] 需要更新的数据索引
     *
     * 举个例子:
     * ```
     *   item1
     *   item2
     *   item3
     *   item4
     *   item5
     * ```
     * 当前界面已经有5个item.
     * 此时需要更新, 页码(updatePage)为2, 数据量(pageSize)为3, 数据(updateDataList)为[i1, i2, i3, i4, i5]
     * 这样第1页的数据就是(pageSize)3条, 对应界面上的[item1, item2, item3], 从第2页开始更新界面[item4, item5, ...]
     * 那么updateOrCreateItem方法执行参数依次是:
     * ```
     *  1: oldItem = item4; data = i1; index = 0;
     *  2: oldItem = item5; data = i2; index = 1;
     *  3: oldItem = null; data = i3; index = 2;
     *  4: oldItem = null; data = i4; index = 3;
     *  5: oldItem = null; data = i5; index = 4;
     * ```
     *
     * @return 返回null, 则会删除对应的[oldItem], 返回与[oldItem]不一样的item, 则会替换原来的[oldItem]
     * */
    var updateOrCreateItem: (oldItem: DslAdapterItem?, data: Any?, itemIndex: Int) -> DslAdapterItem? =
        { oldItem, data, itemIndex ->
            oldItem
        }

    /**数据计算完之后*/
    var adapterUpdateResult: (dslAdapter: DslAdapter) -> Unit = { dslAdapter ->
        with(dslAdapter) {
            if (dataItems.isEmpty() && headerItems.isEmpty() && footerItems.isEmpty()) {
                //空数据
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
            } else {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
            }
        }
        adapterCheckLoadMore(dslAdapter)
    }

    /**加载更多检查*/
    var adapterCheckLoadMore: (dslAdapter: DslAdapter) -> Unit = { dslAdapter ->
        dslAdapter.updateLoadMore(
            updatePage,
            updateSize(),
            pageSize,
            alwaysEnableLoadMore
        )
    }
}

/**轻量差异更新*/
@UpdateFlag
fun UpdateDataConfig.updateData(originList: List<DslAdapterItem>): List<DslAdapterItem> {

    //最后的结果集
    val result = mutableListOf<DslAdapterItem>()

    originList.let {
        //旧数据
        val oldList = ArrayList(it)

        //新数据
        val list = updateDataList ?: emptyList()

        //需要被移除的旧数据集合
        val oldRemoveList = mutableListOf<DslAdapterItem>()
        val newAddList = mutableListOf<DslAdapterItem>()

        val updateStartIndex = max(0, updatePage - startPage) * pageSize
        val updateEndIndex = updateStartIndex + updateSize()

        for (i in updateStartIndex until updateEndIndex) {
            val index = i - updateStartIndex
            val data = list.getOrNull(index)
            val oldItem = oldList.getOrNull(i)
            val newItem = updateOrCreateItem(oldItem, data, index)

            if (newItem != null) {
                newItem.itemUpdateFlag = true
                newItem.itemData = data
            }

            when {
                //remove old item
                newItem == null -> {
                    if (oldItem != null) {
                        oldRemoveList.add(oldItem)
                    }
                }
                //replace old item
                oldItem != null && oldItem != newItem -> {
                    oldList[i] = newItem
                }
                //update old item
                else -> {
                    if (oldItem == null) {
                        newAddList.add(newItem)
                    }
                }
            }
        }

        //超范围的旧数据
        for (i in updateEndIndex until oldList.size) {
            oldRemoveList.add(oldList[i])
        }

        oldList.removeAll(oldRemoveList)

        result.addAll(oldList)
        result.addAll(newAddList)
    }

    return result
}

/**支持相同类型之间的轻量差异更新[headerItems]*/
@UpdateByDiff
fun DslAdapter.updateHeaderData(action: UpdateDataConfig.() -> Unit) {
    val config = UpdateDataConfig()
    config.updatePage = Page.FIRST_PAGE_INDEX
    config.pageSize = Int.MAX_VALUE
    config.action()

    changeHeaderItems(true, config.filterParams) {
        val result = config.updateData(it)
        it.clear()
        it.addAll(result)
    }
}

/**[footerItems]*/
@UpdateByDiff
fun DslAdapter.updateFooterData(action: UpdateDataConfig.() -> Unit) {
    val config = UpdateDataConfig()
    config.updatePage = Page.FIRST_PAGE_INDEX
    config.pageSize = Int.MAX_VALUE
    config.action()

    changeFooterItems(true, config.filterParams) {
        val result = config.updateData(it)
        it.clear()
        it.addAll(result)
    }
}

/**更新指定页码的数据, 支持轻量差异更新.[dataItems]*/
@UpdateByDiff
fun DslAdapter.updateData(action: UpdateDataConfig.() -> Unit) {
    val config = UpdateDataConfig()
    config.action()

    changeDataItems(false, config.filterParams) {
        val result = config.updateData(it)
        it.clear()
        it.addAll(result)
        config.adapterUpdateResult(this)
    }
}

/**简单的判断是否需要替换/更新[oldItem]*/
@UpdateFlag
fun <Item : DslAdapterItem> updateOrCreateItemByClass(
    itemClass: Class<Item>,
    oldItem: DslAdapterItem?,
    initItem: Item.() -> Unit = { }
): DslAdapterItem? {
    var newItem = oldItem
    if (oldItem == null || oldItem.className() != itemClass.className()) {
        newItem = itemClass.newInstance()
    }
    (newItem as? Item?)?.apply {
        this.initItem()
    }
    return newItem
}

/**更新单页数据*/
@UpdateByDiff
inline fun <reified Item : DslAdapterItem> DslAdapter.updateSingleData(
    dataList: List<Any>?,
    requestPage: Int = Page.FIRST_PAGE_INDEX,
    requestPageSize: Int = Int.MAX_VALUE,
    crossinline action: UpdateDataConfig.() -> Unit = {},
    crossinline initItem: Item.(data: Any?) -> Unit = {}
) {
    updateData {
        updatePage = requestPage
        pageSize = requestPageSize
        updateDataList = dataList
        updateOrCreateItem = { oldItem, data, _ ->
            updateOrCreateItemByClass(Item::class.java, oldItem) {
                initItem(data)
            }
        }
        action()
    }
}

@UpdateByDiff
inline fun <reified Item : DslAdapterItem> DslAdapter.updateSingleDataIndex(
    dataList: List<Any>?,
    requestPage: Int = Page.FIRST_PAGE_INDEX,
    requestPageSize: Int = Int.MAX_VALUE,
    crossinline action: UpdateDataConfig.() -> Unit = {},
    crossinline initItem: Item.(data: Any?, index: Int) -> Unit = { _, _ -> }
) {
    updateData {
        updatePage = requestPage
        pageSize = requestPageSize
        updateDataList = dataList
        updateOrCreateItem = { oldItem, data, index ->
            updateOrCreateItemByClass(Item::class.java, oldItem) {
                initItem(data, index)
            }
        }
        action()
    }
}

@UpdateByDiff
fun DslAdapter.toAdapterError(error: Throwable?) {
    updateAdapterErrorState(error)
}

/**更新[DslAdapter]的异常状态*/
@UpdateByDiff
fun DslAdapter.updateAdapterErrorState(error: Throwable?) {
    if (error != null) {
        //加载失败
        when {
            adapterItems.isEmpty() -> {
                dslAdapterStatusItem.itemErrorThrowable = error
                toError()
            }

            isAdapterStatusLoading() -> toNone()
            else -> toLoadMoreError()
        }
    }
}

/**更新[DslAdapter]情感图状态*/
@UpdateByDiff
fun DslAdapter.updateAdapterState(list: List<*>?, error: Throwable?, page: Page = singlePage()) {
    updateAdapterErrorState(error)
    if (error == null) {
        val isDataEmpty: Boolean = if (page.isFirstPage()) {
            list.isNullOrEmpty()
        } else {
            dataItems.isEmpty()
        }

        if (isDataEmpty && headerItems.isEmpty() && footerItems.isEmpty()) {
            //空数据
            updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        } else {
            updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
        }
        updateLoadMore(page.requestPageIndex, list.size(), page.requestPageSize, false)
    }
}

/**
 * 单一数据类型加载完成后, 调用此方法.
 * 自动处理, 情感图切换, 加载更多切换.
 *
 * [itemClass] 渲染界面的`DslAdapterItem`
 * [dataList] 数据列表, 数据bean, 会被自动赋值给`dslAdapter.itemData`成员
 * [error] 是否有错误, 如果有错误, 将会根据已有数据量智能切换到错误情感图, 或者加载更多失败的情况
 * [page] 当前Page参数 包含请求页码, 每页请求数据量
 * [initItem] 根据`Item`的类型, 为自定义的数据结构赋值
 * */
@UpdateByDiff
fun <Item : DslAdapterItem, Bean> DslAdapter.loadDataEnd(
    itemClass: Class<Item>,
    dataList: List<Bean>?,
    error: Throwable?,
    page: Page,
    initItem: Item.(data: Bean) -> Unit = {}
) {
    loadDataEndIndex(itemClass, dataList, error, page) { data, _ ->
        initItem(data)
    }
}

@UpdateByDiff
fun <Item : DslAdapterItem, Bean> DslAdapter.loadDataEndIndex(
    itemClass: KClass<Item>,
    dataList: List<Bean>?,
    error: Throwable?,
    page: Page,
    initItem: Item.(data: Bean, index: Int) -> Unit = { _, _ -> }
) {
    loadDataEndIndex(itemClass.java, dataList, error, page, initItem)
}

@UpdateByDiff
fun <Item : DslAdapterItem, Bean> DslAdapter.loadDataEndIndex(
    itemClass: Class<Item>,
    dataList: List<Bean>?,
    error: Throwable?,
    page: Page,
    initItem: Item.(data: Bean, index: Int) -> Unit = { _, _ -> }
) {
    updateAdapterErrorState(error)

    if (error != null) {
        return
    }

    //加载成功
    page.pageLoadEnd()

    //更新数据源
    updateData {
        startPage = page.firstPageIndex
        updatePage = page.requestPageIndex
        pageSize = page.requestPageSize
        updateDataList = dataList as List<Any>?
        updateOrCreateItem = { oldItem, data, index ->
            updateOrCreateItemByClass(itemClass, oldItem) {
                initItem(data as Bean, index)
            }
        }
    }
}

/**重置[DslAdapter], 重新渲染[DslAdapter.dataItems]数据*/
@UpdateByDiff
fun <T> DslAdapter.resetRender(
    data: T?,
    error: Throwable?,
    page: Page,
    render: DslAdapter.(data: T) -> Unit
) {
    if (error != null) {
        render {
            updateAdapterErrorState(error)
        }
        return
    }

    if (data == null) {
        render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        }
        return
    }

    //加载成功
    page.pageLoadEnd()

    changeDataItems {
        it.clear()
        render(data)
        if (it.isEmpty() && headerItems.isEmpty() && footerItems.isEmpty()) {
            //空数据
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        } else {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
        }
    }
}