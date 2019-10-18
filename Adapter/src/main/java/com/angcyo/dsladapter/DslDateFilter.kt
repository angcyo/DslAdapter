package com.angcyo.dsladapter

import android.os.Handler
import android.os.Looper
import android.support.v7.util.DiffUtil
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class DslDateFilter(val dslAdapter: DslAdapter) {

    /**
     * 过滤后的数据源, 缓存过滤后的数据源, 防止每次都计算.
     *
     * 当有原始数据源发生改变时, 需要调用 [updateFilterItems] 更新过滤后的数据源
     * */
    val filterDataList: MutableList<DslAdapterItem> = mutableListOf()

    //抖动控制
    private val updateDependRunnable = UpdateDependRunnable()

    //异步调度器
    private val asyncExecutor: ExecutorService by lazy {
        Executors.newFixedThreadPool(1)
    }

    //diff操作
    private val diffRunnable = DiffRunnable()
    //diff操作取消
    private var diffSubmit: Future<*>? = null

    private val mainHandler: Handler by lazy {
        Handler(Looper.getMainLooper())
    }

    private val handle: OnceHandler by lazy {
        OnceHandler()
    }

    /**更新过滤后的数据源, 采用的是[android.support.v7.util.DiffUtil]*/
    fun updateFilterItemDepend(params: FilterParams) {
        if (handle.hasCallbacks()) {
            diffRunnable.notifyUpdateDependItem()
        }

        diffRunnable._params = params
        updateDependRunnable._params = params

        if (params.justFilter) {
            params.just = true
            params.async = false
        }

        if (params.just) {
            updateDependRunnable.run()
        } else {
            //确保多次触发, 只有一次被执行
            handle.once(updateDependRunnable)
        }
    }


    /*当前位置, 距离下一个分组头, 还有多少个数据 (startIndex, endIndex)*/
    private fun groupChildSize(originList: List<DslAdapterItem>, startIndex: Int): Int {
        var result = 0

        for (i in (startIndex + 1) until originList.size) {
            val item = originList[i]

            if (item.itemIsGroupHead || dslAdapter.footerItems.indexOf(item) != -1 /*在底部数据列表中*/) {
                result = i - startIndex - 1
                break
            } else if (i == originList.size - 1) {
                result = i - startIndex
                break
            }
        }

        return result
    }

    /**过滤[originList]数据源*/
    open fun filterItemList(originList: List<DslAdapterItem>): MutableList<DslAdapterItem> {
        return filterItemHiddenList(filterItemGroupList(originList))
    }

    /**过滤折叠后后的数据列表*/
    open fun filterItemGroupList(originList: List<DslAdapterItem>): MutableList<DslAdapterItem> {
        val list = mutableListOf<DslAdapterItem>()

        var index = 0

        while (index < originList.size) {
            val item = originList[index]

            //第一条数据, 要么是分组头, 要么是 不受分组管理的子项
            list.add(item)

            if (item.itemIsGroupHead) {
                val childSize = groupChildSize(originList, index)
                index += 1

                if (childSize > 0) {
                    if (item.itemGroupExtend) {
                        //展开
                        for (i in index..(index - 1 + childSize)) {
                            list.add(originList[i])
                        }
                    } else {
                        //折叠
                    }

                    //跳过...child
                    index += childSize
                }
            } else {
                index += 1
            }
        }

        return list
    }

    /**过滤需要被隐藏后的数据列表*/
    open fun filterItemHiddenList(originList: List<DslAdapterItem>): MutableList<DslAdapterItem> {
        val result = mutableListOf<DslAdapterItem>()

        //遍历所有数据, 找出需要隐藏的项
        val hideChildFormItemList = mutableListOf<DslAdapterItem>()

        originList.forEach { currentItem ->

            originList.forEachIndexed { index, subEachItem ->
                if (currentItem.isItemInHiddenList(subEachItem, index)) {
                    hideChildFormItemList.add(subEachItem)
                }
            }

            //表单需要隐藏
            if (currentItem.itemHidden) {
                hideChildFormItemList.add(currentItem)
            }
        }

        var index = 0
        originList.forEachIndexed { i, dslAdapterItem ->
            if (hideChildFormItemList.contains(dslAdapterItem)) {
                //需要隐藏表单 item

                L.v(
                    "${index++}. 隐藏表单:" +
                            "${dslAdapterItem.javaClass.simpleName} " +
                            "${dslAdapterItem.itemTag ?: ""} " +
                            "index:$i"
                )
            } else {
                result.add(dslAdapterItem)
            }
        }

        return result
    }

    /**Diff调用处理*/
    internal inner class DiffRunnable : Runnable {
        var _params: FilterParams? = null
        var _newList: List<DslAdapterItem>? = null
        var _diffResult: DiffUtil.DiffResult? = null

        val _resultRunnable = Runnable {
            //因为是异步操作, 所以在 [dispatchUpdatesTo] 时, 才覆盖 filterDataList 数据源
            _newList?.let {
                filterDataList.clear()
                filterDataList.addAll(it)
            }

            _newList = null

            val updateDependItemList = _getUpdateDependItemList()

            if (_params?.justFilter == true) {
                //仅过滤数据源,不更新界面
            } else {
                //根据diff, 更新adapter
                if (dslAdapter.isAdapterStatus()) {
                    //情感图状态模式, 不刷新界面
                } else if (updateDependItemList.isEmpty() && _params?.updateDependItemWithEmpty == false) {
                    //跳过刷新界面
                } else {
                    _diffResult?.dispatchUpdatesTo(dslAdapter)
                }
            }

            notifyUpdateDependItem(updateDependItemList)

            diffSubmit = null
            _diffResult = null
            _params = null
        }

        override fun run() {
            val diffResult = calculateDiff()
            _diffResult = diffResult

            //回调到主线程
            if (Looper.getMainLooper() == Looper.myLooper()) {
                _resultRunnable.run()
            } else {
                mainHandler.post(_resultRunnable)
            }
        }

        /**计算[Diff]*/
        fun calculateDiff(): DiffUtil.DiffResult {

            //2个数据源
            val oldList = ArrayList(filterDataList)
            val newList = filterItemList(dslAdapter.adapterItems)

            //异步操作, 先保存数据源
            _newList = newList

            //开始计算diff
            val diffResult = DiffUtil.calculateDiff(
                RDiffCallback(
                    oldList,
                    newList,
                    object :
                        RDiffCallback<DslAdapterItem>() {

                        override fun areItemsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreItemsTheSame(
                                _params?.formDslAdapterItem,
                                newData
                            )
                        }

                        override fun areContentsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreContentsTheSame(
                                _params?.formDslAdapterItem,
                                newData
                            )
                        }
                    }
                )
            )

            return diffResult
        }

        fun _getUpdateDependItemList(): List<DslAdapterItem> {
            //需要通知更新的子项
            val notifyChildFormItemList = mutableListOf<DslAdapterItem>()

            _params?.formDslAdapterItem?.let { fromItem ->
                dslAdapter.getValidFilterDataList().forEachIndexed { index, dslAdapterItem ->
                    if (fromItem.isItemInUpdateList(dslAdapterItem, index)) {
                        notifyChildFormItemList.add(dslAdapterItem)
                    }
                }
            }

            return notifyChildFormItemList
        }

        fun notifyUpdateDependItem(itemList: List<DslAdapterItem>) {
            if (_params?.formDslAdapterItem == null) {
                return
            }

            itemList.forEachIndexed { index, dslAdapterItem ->
                dslAdapterItem.apply {
                    onItemUpdateFromInner(_params!!.formDslAdapterItem!!)
                    dslAdapterItem.updateAdapterItem(true)
                }

                L.v("$index. 通知更新:${dslAdapterItem.javaClass.simpleName} ${dslAdapterItem.itemTag}")
            }

            _params = null
        }

        //仅仅只是通知更新被依赖的子项关系
        fun notifyUpdateDependItem() {
            notifyUpdateDependItem(_getUpdateDependItemList())
        }
    }

    /**抖动过滤处理*/
    internal inner class UpdateDependRunnable : Runnable {
        var _params: FilterParams? = null
        override fun run() {
            if (_params == null) {
                return
            }
            diffSubmit?.cancel(true)

            if (_params!!.async) {
                diffSubmit = asyncExecutor.submit(diffRunnable)
            } else {
                diffRunnable.run()
            }
            _params = null
        }
    }
}

data class FilterParams(
    /**
     * 触发更新的来源, 定向更新其子项.
     * */
    val formDslAdapterItem: DslAdapterItem? = null,
    /**
     * 异步执行
     * */
    var async: Boolean = true,
    /**
     * 立即执行
     * */
    var just: Boolean = false,
    /**
     * 只过滤列表数据, 不通知界面操作, 开启此属性.[async=true][just=true]
     * */
    var justFilter: Boolean = false,
    /**
     * 当依赖的[DslAdapterItem]列表为空时, 是否要调用[dispatchUpdatesTo]更新界面
     * */
    var updateDependItemWithEmpty: Boolean = true
)