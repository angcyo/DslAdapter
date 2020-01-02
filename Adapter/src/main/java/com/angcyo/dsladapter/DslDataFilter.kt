package com.angcyo.dsladapter

import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.DiffUtil
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

open class DslDataFilter(val dslAdapter: DslAdapter) {

    /**
     * 过滤后的数据源, 缓存过滤后的数据源, 防止每次都计算.
     *
     * 当有原始数据源发生改变时, 需要调用 [updateFilterItems] 更新过滤后的数据源
     * */
    val filterDataList: MutableList<DslAdapterItem> = mutableListOf()

    val _dispatchUpdatesSet = mutableSetOf<OnDispatchUpdatesListener>()

    /**
     * 可以拦截参与计算[diff]的数据源
     * @param oldDataList 界面显示的数据源
     * @param newDataList 即将显示的数据源
     * @return 需要显示的数据源
     * */
    var onFilterDataList: (oldDataList: List<DslAdapterItem>, newDataList: List<DslAdapterItem>) -> List<DslAdapterItem> =
        { _, newDataList -> newDataList }

    //抖动控制
    private
    val updateDependRunnable = UpdateDependRunnable()

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

    /**更新过滤后的数据源, 采用的是[DiffUtil]*/
    fun updateFilterItemDepend(params: FilterParams) {
        if (handle.hasCallbacks()) {
            diffRunnable.notifyUpdateDependItem()
        }

        var filterParams = params

        if (params.justFilter) {
            filterParams = params.copy(just = true, async = false)
        }

        diffRunnable._params = filterParams
        updateDependRunnable._params = filterParams

        if (filterParams.just) {
            handle.clear()
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
        return filterItemHiddenList(//3级过滤
            filterSubItemList(//2级过滤
                filterItemGroupList(//1级过滤
                    originList
                )
            )
        )
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

                    diffRunnable._params?.let {
                        //包含需要隐藏的item, 也算updateDependItem
                        if (it.fromDslAdapterItem == currentItem) {
                            it.updateDependItemWithEmpty = true
                        }
                    }

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

    /**过滤需要追加或者隐藏的子项*/
    open fun filterSubItemList(originList: List<DslAdapterItem>): MutableList<DslAdapterItem> {
        val result = mutableListOf<DslAdapterItem>()

        originList.forEach { currentItem ->
            val parentList = mutableListOf<DslAdapterItem>()
            val subList = mutableListOf<DslAdapterItem>()
            loadSubItemList(currentItem, parentList, subList)
            result.addAll(subList)
        }

        return result
    }

    /**枚举加载所有子项*/
    open fun loadSubItemList(
        currentItem: DslAdapterItem,
        parentList: MutableList<DslAdapterItem>,
        subList: MutableList<DslAdapterItem>
    ) {
        if (currentItem.itemHidden) {
            //被隐藏了
        } else {
            currentItem.itemParentList = parentList
            subList.add(currentItem)
            if (currentItem.itemGroupExtend) {
                //需要展开
                currentItem.onItemLoadSubList()
                currentItem.itemSubList.forEach {
                    val pList = ArrayList(parentList)
                    pList.add(currentItem)
                    loadSubItemList(it, pList, subList)
                }
            }
        }
    }

    fun addDispatchUpdatesListener(listener: OnDispatchUpdatesListener) {
        _dispatchUpdatesSet.add(listener)
    }

    fun removeDispatchUpdatesListener(listener: OnDispatchUpdatesListener) {
        _dispatchUpdatesSet.remove(listener)
    }

    /**Diff调用处理*/
    internal inner class DiffRunnable : Runnable {
        var _params: FilterParams? = null
        var _newList: List<DslAdapterItem>? = null
        var _diffResult: DiffUtil.DiffResult? = null

        val _resultRunnable = Runnable {
            //因为是异步操作, 所以在 [dispatchUpdatesTo] 时, 才覆盖 filterDataList 数据源

            val oldSize = filterDataList.size
            var newSize = 0

            val diffList = _newList
            _newList = null

            diffList?.let {
                newSize = it.size
                filterDataList.clear()
                filterDataList.addAll(it)
            }

            diffList?.forEach {
                //清空标志
                it.itemChanging = false
            }

            val updateDependItemList = _getUpdateDependItemList()

            //是否调用了[Dispatch]
            var isDispatchUpdatesTo = false

            if (_params?.justFilter == true) {
                //仅过滤数据源,不更新界面
            } else {
                //根据diff, 更新adapter
                if (dslAdapter.isAdapterStatus()) {
                    //情感图状态模式, 不刷新界面
                } else if (updateDependItemList.isEmpty() &&
                    _params?.updateDependItemWithEmpty == false &&
                    oldSize == newSize
                ) {
                    //跳过[dispatchUpdatesTo]刷新界面, 但是要更新自己
                    dslAdapter.notifyItemChanged(_params?.fromDslAdapterItem)
                } else {
                    _diffResult?.dispatchUpdatesTo(dslAdapter)
                    isDispatchUpdatesTo = true
                }
            }

            notifyUpdateDependItem(updateDependItemList)

            if (isDispatchUpdatesTo && _dispatchUpdatesSet.isNotEmpty()) {
                val updatesSet = mutableSetOf<OnDispatchUpdatesListener>()
                updatesSet.addAll(_dispatchUpdatesSet)
                updatesSet.forEach {
                    it.onDispatchUpdatesAfter(dslAdapter)
                }
            }

            diffSubmit = null
            _diffResult = null
            _params = null
        }

        var _startTime = 0L
        override fun run() {
            _startTime = nowTime()
            L.d("开始计算Diff:$_startTime")
            val diffResult = calculateDiff()
            val nowTime = nowTime()
            val s = (nowTime - _startTime) / 100
            val ms = ((nowTime - _startTime) % 100) * 1f / 1000
            L.i("Diff计算耗时:${s + ms}ms")
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
            _newList = onFilterDataList(oldList, newList)

            //开始计算diff
            val diffResult = DiffUtil.calculateDiff(
                RDiffCallback(
                    oldList,
                    _newList,
                    object :
                        RDiffCallback<DslAdapterItem>() {

                        override fun areItemsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreItemsTheSame(
                                _params?.fromDslAdapterItem,
                                newData
                            )
                        }

                        override fun areContentsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreContentsTheSame(
                                _params?.fromDslAdapterItem,
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

            _params?.fromDslAdapterItem?.let { fromItem ->
                dslAdapter.getValidFilterDataList().forEachIndexed { index, dslAdapterItem ->
                    if (fromItem.isItemInUpdateList(dslAdapterItem, index)) {
                        notifyChildFormItemList.add(dslAdapterItem)
                    }
                }
            }

            return notifyChildFormItemList
        }

        fun notifyUpdateDependItem(itemList: List<DslAdapterItem>) {
            if (_params?.fromDslAdapterItem == null) {
                return
            }

            itemList.forEachIndexed { index, dslAdapterItem ->
                dslAdapterItem.apply {
                    onItemUpdateFromInner(_params!!.fromDslAdapterItem!!)
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
    val fromDslAdapterItem: DslAdapterItem? = null,
    /**
     * 异步计算Diff
     * */
    var async: Boolean = true,
    /**
     * 立即执行, 不检查抖动
     * */
    var just: Boolean = false,
    /**
     * 只过滤列表数据, 不通知界面操作, 开启此属性.[async=true][just=true]
     * */
    var justFilter: Boolean = false,
    /**
     * 前提, Diff 之后, 2个数据列表的大小要一致.
     *
     * 当依赖的[DslAdapterItem] [isItemInUpdateList]列表为空时, 是否要调用[dispatchUpdatesTo]更新界面
     * */
    var updateDependItemWithEmpty: Boolean = true
)

interface OnDispatchUpdatesListener {
    /**
     * 当触发了[dispatchUpdatesTo]后回调
     * */
    fun onDispatchUpdatesAfter(dslAdapter: DslAdapter)
}