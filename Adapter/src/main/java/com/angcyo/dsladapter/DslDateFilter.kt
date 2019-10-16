package com.angcyo.dsladapter

import android.support.v7.util.DiffUtil
import rx.Observable
import rx.Observer
import rx.Subscription
import rx.functions.Action1
import rx.observables.SyncOnSubscribe

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/05/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

open class DslDateFilter(val adapter: DslAdapter) {

    /**
     * 过滤后的数据源, 缓存过滤后的数据源, 防止每次都计算.
     *
     * 当有原始数据源发生改变时, 需要调用 [updateFilterItems] 更新过滤后的数据源
     * */
    val filterDataList: MutableList<DslAdapterItem> = mutableListOf()

    /**diff订阅*/
    private var subscribe: Subscription? = null

    //需要执行的diff算法操作
    private val diffSubscribe = DiffSubscribe()
    //diff之后的结果
    private val diffResultAction = DiffResultAction()
    //抖动控制
    private val updateDependRunnable = UpdateDependRunnable()

    private val handle: OnceHandler by lazy {
        OnceHandler()
    }

    /**更新过滤后的数据源, 采用的是[android.support.v7.util.DiffUtil]*/
    @Deprecated("2019-10-16")
    fun updateFilterItemDepend(
        formDslAdapterItem: DslAdapterItem? = null,
        async: Boolean = true,
        just: Boolean = false //立即执行
    ) {
        updateFilterItemDepend(FilterParams(formDslAdapterItem, async, just))
    }

    fun updateFilterItemDepend(params: FilterParams) {
        if (handle.hasCallbacks()) {
            diffResultAction.notifyUpdateDependItem()
        }

        diffResultAction._params = params
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

            if (item.itemIsGroupHead || adapter.footerItems.indexOf(item) != -1 /*在底部数据列表中*/) {
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

    internal inner class DiffSubscribe : SyncOnSubscribe<Int, DiffUtil.DiffResult>() {

        override fun generateState(): Int {
            return 1
        }

        override fun next(state: Int, observer: Observer<in DiffUtil.DiffResult>): Int? {
            observer.onNext(calculateDiff())
            observer.onCompleted()
            return 0
        }

        /**计算[Diff]*/
        fun calculateDiff(): DiffUtil.DiffResult {

            //2个数据源
            val oldList = ArrayList(filterDataList)
            val newList = filterItemList(adapter.adapterItems)

            //异步操作, 先保存数据源
            diffResultAction._newList = newList

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
                            return oldData.thisAreItemsTheSame(newData)
                        }

                        override fun areContentsTheSame(
                            oldData: DslAdapterItem,
                            newData: DslAdapterItem
                        ): Boolean {
                            return oldData.thisAreContentsTheSame(newData)
                        }
                    }
                )
            )

            return diffResult
        }
    }

    internal inner class DiffResultAction : Action1<DiffUtil.DiffResult> {
        var _params: FilterParams? = null

        var _newList: List<DslAdapterItem>? = null

        override fun call(diffResult: DiffUtil.DiffResult) {

            //因为是异步操作, 所以在 [dispatchUpdatesTo] 时, 才覆盖 filterDataList 数据源
            _newList?.let {
                filterDataList.clear()
                filterDataList.addAll(it)
            }

            _newList = null

            if (_params?.justFilter == true) {
            } else {
                //根据diff, 更新adapter
                diffResult.dispatchUpdatesTo(adapter)
            }

            notifyUpdateDependItem()

            subscribe = null
        }

        //仅仅只是通知更新被依赖的子项关系
        fun notifyUpdateDependItem() {
            if (_params?.formDslAdapterItem == null) {
                return
            }
            //需要通知更新的子项
            val notifyChildFormItemList = mutableListOf<DslAdapterItem>()
            adapter.getValidFilterDataList().forEachIndexed { index, dslAdapterItem ->
                if (_params?.formDslAdapterItem!!.isItemInUpdateList(dslAdapterItem, index)) {
                    notifyChildFormItemList.add(dslAdapterItem)
                }
            }

            var index = 0
            notifyChildFormItemList.forEach {
                it.apply {
                    onItemUpdateFromInner(_params?.formDslAdapterItem!!)
                    it.updateAdapterItem(true)
                }

                L.v(
                    "${index++}. 通知更新:${it.javaClass.simpleName} ${it.itemTag ?: ""}"
                )
            }

            _params = null
        }
    }

    internal inner class UpdateDependRunnable : Runnable {
        var _params: FilterParams? = null
        override fun run() {
            if (_params == null) {
                return
            }
            subscribe?.unsubscribe()

            if (_params!!.async) {
                subscribe = Observable
                    .create(diffSubscribe)
                    .compose<DiffUtil.DiffResult>(Rx.defaultTransformer<DiffUtil.DiffResult>())
                    .subscribe(diffResultAction)
            } else {
                diffResultAction.call(diffSubscribe.calculateDiff())
            }
            _params = null
        }
    }
}

data class FilterParams(
    val formDslAdapterItem: DslAdapterItem? = null, //定向更新其子项
    var async: Boolean = true, //异步执行
    var just: Boolean = false, //立即执行
    var justFilter: Boolean = false //只过滤列表数据, 不通知界面操作, 开启此属性.[async=true][just=true]
)