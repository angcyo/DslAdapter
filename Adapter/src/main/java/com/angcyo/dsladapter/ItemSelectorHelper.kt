package com.angcyo.dsladapter

import android.support.annotation.IntDef
import android.support.v7.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

/**
 * 单选/多选 实现类
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class ItemSelectorHelper(val dslAdapter: DslAdapter) {

    /**固定选项, 这里的数据不允许被操作, 切强制选中状态*/
    var fixedSelectorItemList: List<DslAdapterItem>? = null
        set(value) {
            field = value

            field?.let {
                selector(
                    it,
                    SelectorParams(
                        it.lastOrNull(),
                        selector = OPTION_SELECT,
                        notify = true,
                        notifyItemChange = true,
                        updateItemDepend = true
                    )
                )
            }

        }

    /**设置的选择模式*/
    @MODEL
    var selectorModel = MODEL_NORMAL
        set(value) {
            val old = field
            field = value

            if (old != value) {
                onItemSelectorListener.onSelectorModelChange(old, value)
            }
        }

    /**事件监听*/
    var onItemSelectorListener: OnItemSelectorListener = object : OnItemSelectorListener {}

    /**
     * 选择/取消 单项
     * @param selectorParams 参数
     * */
    fun selector(selectorParams: SelectorParams) {
        if (selectorParams.item == null) {
            return
        }
        _check {
            val item = selectorParams.item!!
            val isSelectorItem = _isSelectItem(selectorParams)
            when {
                _isInFiexedList(item) -> {
                    //在固定列表中
                    _selector(selectorParams.apply {
                        selector = OPTION_SELECT
                    })
                }
                item.isItemCanSelected(item.itemIsSelected, isSelectorItem) -> {
                    _selector(selectorParams)
                }
                else -> {
                    //不允许被选中
                }
            }
        }
    }

    //是否是需要选中目标
    fun _isSelectItem(selectorParams: SelectorParams): Boolean {
        return when {
            selectorParams.item == null -> false
            selectorParams.selector == OPTION_SELECT -> true
            selectorParams.selector == OPTION_MUTEX -> !selectorParams.item!!.itemIsSelected
            else -> false
        }
    }

    //是否在固定列表中
    fun _isInFiexedList(item: DslAdapterItem? = null): Boolean {
        return item?.run {
            fixedSelectorItemList?.contains(item)
        } ?: false
    }

    /**
     * 选择/取消 多个项.
     * 单选模式下, 只会选择最后一个
     * */
    fun selector(itemList: List<DslAdapterItem>, selectorParams: SelectorParams) {
        itemList.forEach {
            selector(SelectorParams(it, selectorParams.selector, false))
        }
        if (selectorParams.notify) {
            if (itemList.isEmpty() && !selectorParams.notifyWithListEmpty) {
            } else {
                _notifySelectorChange(selectorParams)
            }
        }
    }

    /**
     * 选择/取消 一个范围
     * */
    fun selector(indexRange: IntRange, selectorParams: SelectorParams) {
        val allItems = dslAdapter.getDataList(selectorParams._useFilterList)
        val list = mutableListOf<DslAdapterItem>()

        //修正一下范围
        val range = IntRange(
            min(indexRange.first, indexRange.last),
            max(indexRange.first, indexRange.last)
        )

        for (index in range) {
            if (index in allItems.indices) {
                list.add(allItems[index])
            }
        }

        selector(list, selectorParams)
    }

    /**
     * 选择/取消 全部
     * */
    fun selectorAll(selectorParams: SelectorParams) {
        selector(dslAdapter.getDataList(selectorParams._useFilterList), selectorParams)
    }

    fun _selector(selectorParams: SelectorParams) {
        if (selectorParams.item == null) {
            return
        }
        val isSelectorItem = _isSelectItem(selectorParams)
        val item = selectorParams.item!!

        if (item.itemIsSelected == isSelectorItem) {
            //重复操作
            return
        }

        if (_isInFiexedList(item)) {
            //直接操作, 跳过判断条件
            _selectorInner(selectorParams)
        } else {
            val oldSelectorList = getSelectorItemList(selectorParams._useFilterList)
            if (selectorModel == MODEL_SINGLE) {
                //单选
                if (oldSelectorList.isNotEmpty()) {
                    //取消之前选中的项
                    oldSelectorList.forEach {
                        if (it != item) {
                            if (it.isItemCanSelected(it.itemIsSelected, false)) {
                                _selectorInner(
                                    SelectorParams(
                                        it,
                                        selector = OPTION_DESELECT,
                                        notify = true
                                    )
                                )
                            }
                        }
                    }
                }
                _selectorInner(selectorParams)
            } else if (selectorModel == MODEL_MULTI) {
                //多选
                _selectorInner(selectorParams)
            }
        }

        if (selectorParams.notify) {
            //事件通知
            _notifySelectorChange(selectorParams)
        }
    }

    fun _selectorInner(selectorParams: SelectorParams) {
        if (selectorParams.item == null) {
            return
        }

        val isSelectorItem = _isSelectItem(selectorParams)

        if (!isSelectorItem) {
            //取消选择
            if (_isInFiexedList(selectorParams.item)) {
                //在固定列表中, 不允许取消
                return
            }
        }

        val item = selectorParams.item!!
        item.itemIsSelected = isSelectorItem
        if (selectorParams.notifyItemChange) {
            item._itemSelectorChange(selectorParams)
        }
        dslAdapter.notifyItemChanged(item)
    }

    fun _check(doIt: () -> Unit) {
        if (selectorModel != MODEL_NORMAL) {
            doIt()
        } else {
            L.w("当前选择模式[${selectorModel._modelToString()}]不支持操作.")
        }
    }

    fun _notifySelectorChange(selectorParams: SelectorParams) {
        val selectorItemList = mutableListOf<DslAdapterItem>()
        val selectorIndexList = mutableListOf<Int>()
        var isSelectorAll = false
        dslAdapter.getDataList(selectorParams._useFilterList).apply {
            forEachIndexed { index, dslAdapterItem ->
                if (dslAdapterItem.itemIsSelected) {
                    selectorIndexList.add(index)
                    selectorItemList.add(dslAdapterItem)
                }
            }
            isSelectorAll = size > 0 && size == selectorItemList.size
        }
        onItemSelectorListener.onSelectorItemChange(
            selectorItemList,
            selectorIndexList,
            isSelectorAll,
            selectorParams
        )
    }

    /**获取所有选中的项*/
    fun getSelectorItemList(useFilterList: Boolean = true): MutableList<DslAdapterItem> {
        val result = mutableListOf<DslAdapterItem>()
        dslAdapter.getDataList(useFilterList).filterTo(result) {
            it.itemIsSelected
        }
        return result
    }
}

/** 正常 状态 */
const val MODEL_NORMAL = 0

/** 单选 状态 */
const val MODEL_SINGLE = 1

/** 多选 状态 */
const val MODEL_MULTI = 2

@IntDef(MODEL_NORMAL, MODEL_SINGLE, MODEL_MULTI)
@Retention(AnnotationRetention.SOURCE)
annotation class MODEL

/**互斥选择*/
const val OPTION_MUTEX = 0

/**选择*/
const val OPTION_SELECT = 1

/**取消*/
const val OPTION_DESELECT = 2

@IntDef(OPTION_MUTEX, OPTION_SELECT, OPTION_DESELECT)
@Retention(AnnotationRetention.SOURCE)
annotation class SELECTOR

data class SelectorParams(

    /**操作的 目标*/
    var item: DslAdapterItem? = null,

    /**操作*/
    @SELECTOR
    var selector: Int = OPTION_SELECT,

    /**
     * 是否要通知事件
     * [com.angcyo.dsladapter.ItemSelectorHelper._notifySelectorChange]
     * */
    var notify: Boolean = true,

    /**
     * 是否需要回调[_itemSelectorChange]
     * [com.angcyo.dsladapter.ItemSelectorHelper._selectorInner]
     * */
    var notifyItemChange: Boolean = true,

    /**
     * 传递给
     * [com.angcyo.dsladapter.DslAdapterItem._itemSelectorChange]
     * */
    var updateItemDepend: Boolean = false,

    //额外自定义的扩展数据
    var extend: Any? = null,

    //使用过滤后的数据源
    var _useFilterList: Boolean = true,

    /**
     * 当调用
     * [selector(kotlin.ranges.IntRange, com.angcyo.dsladapter.SelectorParams)]
     * or
     * [selector(java.util.List<? extends com.angcyo.dsladapter.DslAdapterItem>, com.angcyo.dsladapter.SelectorParams)]
     * 操作列表为空时, 是否继续通知事件.
     *
     * 需要优先开启[notify]
     * */
    var notifyWithListEmpty: Boolean = false
)

public fun Boolean.toSelectOption(): Int = if (this) OPTION_SELECT else OPTION_DESELECT

private fun Int._modelToString(): String {
    return when (this) {
        MODEL_SINGLE -> "MODEL_SINGLE"
        MODEL_MULTI -> "MODEL_MULTI"
        else -> "MODEL_NORMAL"
    }
}

interface OnItemSelectorListener {

    /**选择模式改变*/
    fun onSelectorModelChange(@MODEL from: Int, @MODEL to: Int) {
        L.i("选择模式改变:[${from._modelToString()}]->[${to._modelToString()}]")
    }

    /**
     * @param selectorItems 选中的项
     * @param selectorIndexList 选中项在数据源中的索引
     * @param isSelectorAll 是否全部选中
     * */
    fun onSelectorItemChange(
        selectorItems: MutableList<DslAdapterItem>,
        selectorIndexList: MutableList<Int>,
        isSelectorAll: Boolean,
        selectorParams: SelectorParams
    ) {
        L.i("选择改变->${selectorIndexList}")
    }
}

public fun DslAdapter?.normalModel() {
    this?.itemSelectorHelper?.selectorModel = MODEL_NORMAL
}

public fun DslAdapter?.singleModel() {
    this?.itemSelectorHelper?.selectorModel = MODEL_SINGLE
}

public fun DslAdapter?.multiModel() {
    this?.itemSelectorHelper?.selectorModel = MODEL_MULTI
}

public fun RecyclerView?.normalModel() {
    (this?.adapter as? DslAdapter)?.normalModel()
}

public fun RecyclerView?.singleModel() {
    (this?.adapter as? DslAdapter)?.singleModel()
}

public fun RecyclerView?.multiModel() {
    (this?.adapter as? DslAdapter)?.multiModel()
}
