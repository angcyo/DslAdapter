package com.angcyo.dsladapter.data

import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.annotation.UpdateByDiff
import com.angcyo.dsladapter.annotation.UpdateFlag
import kotlin.math.max

/**
 * 请使用[UpdateDataConfig]相关处理方法
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/05
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

/**用于[Adapter]中单一数据类型的列表*/
@UpdateByDiff
inline fun <reified Item : DslAdapterItem> DslAdapter.loadSingleData(
    dataList: List<Any>?,
    page: Int = Page.FIRST_PAGE_INDEX,
    pageSize: Int = Int.MAX_VALUE,
    filterParams: FilterParams = defaultFilterParams!!.apply {
        payload = listOf(
            DslAdapterItem.PAYLOAD_UPDATE_PART,
            DslAdapterItem.PAYLOAD_UPDATE_MEDIA
        )
    },
    crossinline initOrCreateDslItem: (oldItem: Item?, data: Any) -> Item
) {
    changeDataItems(false, filterParams) {
        //移除所有不同类型的item
        val removeItemList = mutableListOf<DslAdapterItem>()
        it.forEach { item ->
            if (item !is Item) {
                removeItemList.add(item)
            }
        }
        it.removeAll(removeItemList)

        //加载数据
        val list = dataList ?: emptyList()
        //第一页 数据检查
        if (page <= Page.FIRST_PAGE_INDEX) {
            if (it.size > list.size) {
                for (i in max(it.lastIndex, 0) downTo max(list.size, 0)) {
                    it.removeAt(i)
                }
            }
            //重新旧数据
            it.forEachIndexed { index, dslAdapterItem ->
                val data = list[index]
                dslAdapterItem.itemUpdateFlag = dslAdapterItem.itemData != data
                dslAdapterItem.itemData = data
                initOrCreateDslItem(dslAdapterItem as Item, data)
            }
            if (list.size > it.size) {
                //需要补充新的DslAdapterItem
                for (i in it.size until list.size) {
                    val data = list[i]
                    val dslItem = initOrCreateDslItem(null, data)
                    dslItem.itemData = data
                    it.add(dslItem)
                }
            }
            if (it.isEmpty() && headerItems.isEmpty() && footerItems.isEmpty()) {
                //空数据
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
            } else {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                if (dslLoadMoreItem.itemStateEnable) {
                    if (it.size < pageSize) {
                        setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE)
                    } else {
                        setLoadMore(DslLoadMoreItem.LOAD_MORE_NORMAL)
                    }
                }
            }
        } else {
            //第二页 追加数据检查
            for (data in list) {
                val dslItem = initOrCreateDslItem(null, data)
                dslItem.itemData = data
                it.add(dslItem)
            }
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
            if (dslLoadMoreItem.itemStateEnable) {
                if (list.size < pageSize) {
                    setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE)
                } else {
                    setLoadMore(DslLoadMoreItem.LOAD_MORE_NORMAL)
                }
            }
        }
    }
}

/**
 * 与[loadSingleData]的不同在于Dsl的参数不一样
 * */
@UpdateByDiff
inline fun <reified Item : DslAdapterItem> DslAdapter.loadSingleData2(
    dataList: List<Any>?,
    page: Int = Page.FIRST_PAGE_INDEX,
    pageSize: Int = Int.MAX_VALUE,
    filterParams: FilterParams = defaultFilterParams!!.apply {
        payload = listOf(
            DslAdapterItem.PAYLOAD_UPDATE_PART,
            DslAdapterItem.PAYLOAD_UPDATE_MEDIA
        )
    },
    crossinline initItem: Item.(data: Any) -> Unit = {}
) {
    loadSingleData<Item>(dataList, page, pageSize, filterParams) { oldItem, data ->
        (oldItem ?: Item::class.java.newInstance()).apply {
            initItem(data)
        }
    }
}

/**智能设置加载更多的状态和激活状态*/
@UpdateFlag
fun DslAdapter.updateLoadMore(
    updatePage: Int /*当前数据页*/,
    updateSize: Int /*数据页数据更新量*/,
    pageSize: Int = Page.PAGE_SIZE /*数据页数据最大量*/,
    alwaysEnable: Boolean = false /*是否一直激活加载更多, 不管第一页数据不够*/
) {
    if (updatePage <= Page.FIRST_PAGE_INDEX) {
        //更新第一页的数据
        if (updateSize < pageSize) {
            //数据不够, 关闭加载更多
            if (alwaysEnable) {
                setLoadMoreEnable(true)
                setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE)
            } else {
                setLoadMoreEnable(false)
            }
        } else {
            //激活加载更多, 初始化默认状态
            setLoadMoreEnable(true)
            setLoadMore(DslLoadMoreItem.LOAD_MORE_NORMAL)
        }
    } else {
        //更新其他页数据
        if (dslLoadMoreItem.itemStateEnable) {
            if (updateSize < pageSize) {
                setLoadMore(DslLoadMoreItem.LOAD_MORE_NO_MORE)
            } else {
                setLoadMore(DslLoadMoreItem.LOAD_MORE_NORMAL, notify = false)
            }
        }
    }
}