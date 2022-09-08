package com.angcyo.dsladapter

import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.MODEL_MULTI
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.MODEL_NORMAL
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.MODEL_SINGLE
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.OPTION_DESELECT
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.OPTION_MUTEX
import com.angcyo.dsladapter.ItemSelectorHelper.Companion.OPTION_SELECT
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min

/**
 * 单选/多选 实现类
 *
 * https://github.com/angcyo/DslAdapter/wiki/%E5%8D%95%E9%80%89-%E5%A4%9A%E9%80%89-%E6%BB%91%E5%8A%A8%E9%80%89%E6%8B%A9
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class ItemSelectorHelper(val dslAdapter: DslAdapter) {

    companion object {

        /** 正常 状态 */
        const val MODEL_NORMAL = 0

        /** 单选 状态 */
        const val MODEL_SINGLE = 1

        /** 多选 状态 */
        const val MODEL_MULTI = 2

        /**互斥选择*/
        const val OPTION_MUTEX = 0

        /**选择*/
        const val OPTION_SELECT = 1

        /**取消*/
        const val OPTION_DESELECT = 2

    }

    /**固定选项, 这里的数据不允许被操作, 强制选中状态*/
    var fixedSelectorItemList: List<DslAdapterItem>? = null
        set(value) {
            field = value

            field?.let {
                selector(
                    it,
                    SelectorParams(
                        it.lastOrNull(),
                        selector = OPTION_SELECT,
                        notifySelectListener = true,
                        notifyItemSelectorChange = true,
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
                onItemSelectorListenerList.forEach { it.onSelectorModelChange(old, value) }
                onItemSelectorListener?.onSelectorModelChange(old, value)
            }
        }

    /**事件监听*/
    var onItemSelectorListener: OnItemSelectorListener? = null

    val onItemSelectorListenerList: CopyOnWriteArrayList<OnItemSelectorListener> =
        CopyOnWriteArrayList()

    //<editor-fold desc="选择操作">

    /**
     * 选择/取消 单项
     * @param selectorParams 参数
     * */
    fun selector(selectorParams: SelectorParams) {
        _checkModel {
            val item = selectorParams.item
            val isSelectorItem = _isSelectItem(selectorParams)
            when {
                //空item操作
                item == null -> _selector(selectorParams)
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
                    //不允许被选择
                }
            }
        }
    }

    /**
     * 选择/取消 多个项.
     * 单选模式下, 只会选择最后一个
     * */
    fun selector(
        itemList: List<DslAdapterItem>,
        selectorParams: SelectorParams = SelectorParams()
    ) {
        itemList.forEach {
            selector(SelectorParams(it, selectorParams.selector, false))
        }
        if (selectorParams.notifySelectListener) {
            if (itemList.isEmpty() && !selectorParams.notifyWithListEmpty) {
            } else {
                _notifySelectorChange(selectorParams)
            }
        }
    }

    fun selector(dslAdapterItem: DslAdapterItem?) {
        selector(dslAdapterItem, dslAdapterItem?.itemIsSelected != true)
    }

    /**
     * 选择/取消 一个
     * */
    fun selector(dslAdapterItem: DslAdapterItem?, selected: Boolean = true) {
        val selectorParams = SelectorParams()
        selectorParams.item = dslAdapterItem
        selectorParams.selector = selected.toSelectOption()
        selector(selectorParams)
    }

    /**
     * 选择/取消 一个
     * */
    fun selector(position: Int, selectorParams: SelectorParams = SelectorParams()) {
        val allItems = dslAdapter.getDataList(selectorParams._useFilterList)
        selectorParams.item = allItems.getOrNull(position)
        selector(selectorParams)
    }

    fun selector(dslAdapterItem: DslAdapterItem?, action: SelectorParams.() -> Unit = {}) {
        val selectorParams = SelectorParams()
        selectorParams.item = dslAdapterItem
        selectorParams.selector = OPTION_MUTEX
        selectorParams.action()
        selector(selectorParams)
    }

    /**
     * 选择/取消 一个范围
     * */
    fun selector(indexRange: IntRange, selectorParams: SelectorParams = SelectorParams()) {
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
    fun selectorAll(selectorParams: SelectorParams = SelectorParams()) {
        selector(dslAdapter.getDataList(selectorParams._useFilterList), selectorParams)
    }

    //</editor-fold desc="选择操作">

    //<editor-fold desc="内部操作">

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

    fun _selector(selectorParams: SelectorParams) {
        val isSelectorItem = _isSelectItem(selectorParams)
        val item = selectorParams.item

        if (item != null && item.itemIsSelected == isSelectorItem) {
            //重复操作
            return
        }

        if (item != null && _isInFiexedList(item)) {
            //直接操作, 跳过判断条件
            _selectorInner(selectorParams)
        } else {
            val oldSelectorList = getSelectorItemList(selectorParams._useFilterList)
            if (selectorModel == MODEL_SINGLE) {
                //单选模式下, 选中一个null item, 允许取消之前的item

                //单选模式下, 取消sub item中的状态
                val allItems = dslAdapter.getDataList(selectorParams._useFilterList)
                allItems.forEach {
                    _cancelSubItemList(it, item)
                }

                if (oldSelectorList.isNotEmpty()) {
                    //取消之前选中的项
                    oldSelectorList.forEach {
                        if (item == null || it != item) {
                            if (it.isItemCanSelected(it.itemIsSelected, false)) {
                                _selectorInner(
                                    SelectorParams(
                                        it,
                                        selector = OPTION_DESELECT,
                                        notifySelectListener = true
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

        if (selectorParams.notifySelectListener) {
            //事件通知
            _notifySelectorChange(selectorParams)
        }
    }

    /**取消*/
    fun _cancelSubItemList(item: DslAdapterItem, fromItem: DslAdapterItem?) {
        item.itemSubList.forEach { subItem ->
            if (subItem.itemIsSelected && subItem != fromItem) {
                if (subItem.isItemCanSelected(subItem.itemIsSelected, false)) {
                    _selectorInner(
                        SelectorParams(
                            fromItem,
                            selector = OPTION_DESELECT,
                            notifySelectListener = true
                        )
                    )
                }
            }
            //递归
            _cancelSubItemList(subItem, fromItem)
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
        if (selectorParams.notifyItemSelectorChange) {
            item._itemSelectorChange(selectorParams)
        }

        if (selectorParams.notifyItemChanged) {
            dslAdapter.notifyItemChanged(
                item,
                selectorParams.payload,
                selectorParams._useFilterList
            )
        }
    }

    fun _checkModel(doIt: () -> Unit) {
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

        //通知listener
        onItemSelectorListenerList.forEach {
            it.onSelectorItemChange(
                selectorItemList,
                selectorIndexList,
                isSelectorAll,
                selectorParams
            )
        }
        onItemSelectorListener?.onSelectorItemChange(
            selectorItemList,
            selectorIndexList,
            isSelectorAll,
            selectorParams
        )
    }

    //</editor-fold desc="内部操作">

    //<editor-fold desc="其他操作">

    /**获取所有选中项的列表*/
    fun getSelectorItemList(useFilterList: Boolean = true): List<DslAdapterItem> {
        val result = mutableListOf<DslAdapterItem>()
        dslAdapter.getDataList(useFilterList).filterTo(result) {
            it.itemIsSelected
        }
        return result
    }

    /**获取所有选中项的索引列表*/
    fun getSelectorIndexList(useFilterList: Boolean = true): List<Int> {
        val result = mutableListOf<Int>()
        dslAdapter.getDataList(useFilterList).apply {
            forEachIndexed { index, dslAdapterItem ->
                if (dslAdapterItem.itemIsSelected) {
                    result.add(index)
                }
            }
        }
        return result
    }

    fun observer(config: DslItemSelectorListener.() -> Unit): OnItemSelectorListener {
        return DslItemSelectorListener().apply {
            config()
            addObserver(this)
        }
    }

    fun addObserver(listener: OnItemSelectorListener) {
        onItemSelectorListenerList.add(listener)
    }

    fun removeObserver(listener: OnItemSelectorListener) {
        onItemSelectorListenerList.remove(listener)
    }

    //</editor-fold desc="其他操作">
}

@IntDef(
    MODEL_NORMAL,
    MODEL_SINGLE,
    MODEL_MULTI
)
@Retention(AnnotationRetention.SOURCE)
annotation class MODEL


@IntDef(
    OPTION_MUTEX,
    OPTION_SELECT,
    OPTION_DESELECT
)
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
    var notifySelectListener: Boolean = true,

    /**
     * 是否需要回调[_itemSelectorChange]
     * [com.angcyo.dsladapter.ItemSelectorHelper._selectorInner]
     * */
    var notifyItemSelectorChange: Boolean = true,

    /**
     * 传递给
     * [com.angcyo.dsladapter.DslAdapterItem._itemSelectorChange]
     * */
    var updateItemDepend: Boolean = false,

    //额外自定义的扩展数据, 自定义传递使用
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
     * 需要优先开启[notifySelectListener]
     * */
    var notifyWithListEmpty: Boolean = false,

    /**是否要调用[androidx.recyclerview.widget.RecyclerView.Adapter.notifyItemChanged(int, java.lang.Object)]*/
    var notifyItemChanged: Boolean = true,

    /**参考[androidx.recyclerview.widget.RecyclerView.Adapter.onBindViewHolder(VH, int, java.util.List<java.lang.Object>)]*/
    var payload: Any? = listOf(
        DslAdapterItem.PAYLOAD_UPDATE_PART,
        DslAdapterItem.PAYLOAD_UPDATE_SELECT
    )
)

fun Boolean.toSelectOption(): Int = if (this) OPTION_SELECT else OPTION_DESELECT

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

class DslItemSelectorListener : OnItemSelectorListener {
    var onModelChange: ((from: Int, to: Int) -> Unit)? = null

    var onItemChange: ((
        selectorItems: MutableList<DslAdapterItem>,
        selectorIndexList: MutableList<Int>,
        isSelectorAll: Boolean,
        selectorParams: SelectorParams
    ) -> Unit)? = null

    override fun onSelectorModelChange(from: Int, to: Int) {
        onModelChange?.invoke(from, to) ?: super.onSelectorModelChange(from, to)
    }

    override fun onSelectorItemChange(
        selectorItems: MutableList<DslAdapterItem>,
        selectorIndexList: MutableList<Int>,
        isSelectorAll: Boolean,
        selectorParams: SelectorParams
    ) {
        onItemChange?.invoke(selectorItems, selectorIndexList, isSelectorAll, selectorParams)
            ?: super.onSelectorItemChange(
                selectorItems,
                selectorIndexList,
                isSelectorAll,
                selectorParams
            )
    }
}

fun DslAdapter?.normalModel() {
    this?.itemSelectorHelper?.selectorModel = MODEL_NORMAL
}

fun DslAdapter?.singleModel() {
    this?.itemSelectorHelper?.selectorModel = MODEL_SINGLE
}

fun DslAdapter?.multiModel() {
    this?.itemSelectorHelper?.selectorModel = MODEL_MULTI
}

fun RecyclerView?.normalModel() {
    (this?.adapter as? DslAdapter)?.normalModel()
}

fun RecyclerView?.singleModel() {
    (this?.adapter as? DslAdapter)?.singleModel()
}

fun RecyclerView?.multiModel() {
    (this?.adapter as? DslAdapter)?.multiModel()
}

/**获取选中项列表*/
fun DslAdapter.getSelectorItemList(useFilterList: Boolean = true): List<DslAdapterItem> =
    selector().getSelectorItemList(useFilterList)

fun DslAdapter.getSelectorIndexList(useFilterList: Boolean = true): List<Int> =
    selector().getSelectorIndexList(useFilterList)

/**选择改变的监听回调*/
fun DslAdapter.onSelectorChangeListener(
    action: (
        selectorItems: MutableList<DslAdapterItem>,
        selectorIndexList: MutableList<Int>,
        isSelectorAll: Boolean,
        selectorParams: SelectorParams
    ) -> Unit
) {
    selector().onItemSelectorListener = object : OnItemSelectorListener {
        override fun onSelectorItemChange(
            selectorItems: MutableList<DslAdapterItem>,
            selectorIndexList: MutableList<Int>,
            isSelectorAll: Boolean,
            selectorParams: SelectorParams
        ) {
            action(selectorItems, selectorIndexList, isSelectorAll, selectorParams)
        }
    }
}

fun DslAdapter.observerSelectorChangeListener(
    action: (
        selectorItems: MutableList<DslAdapterItem>,
        selectorIndexList: MutableList<Int>,
        isSelectorAll: Boolean,
        selectorParams: SelectorParams
    ) -> Unit
) {
    selector().observer {
        onItemChange = action
    }
}

fun DslAdapterItem.select(action: SelectorParams.() -> Unit = {}) {
    itemDslAdapter?.selector()?.selector(this, action)
}

/**快速获取[ItemSelectorHelper]*/
fun DslAdapter.selector(): ItemSelectorHelper {
    return this.itemSelectorHelper
}

/**快速选择/取消[ItemSelectorHelper]*/
fun DslAdapter.select(
    selected: Boolean = true,
    predicate: (DslAdapterItem) -> Boolean
): DslAdapterItem? {
    return findItem(true, predicate)?.run {
        selector().selector(SelectorParams(this, selected.toSelectOption()))
        this
    }.elseNull {
        L.w("未找到需要选择操作的[DslAdapterItem]")
    }
}

/**快速选择/取消[dslItem]*/
fun DslAdapter.select(dslItem: DslAdapterItem, selected: Boolean = true) {
    selector().selector(SelectorParams(dslItem, selected.toSelectOption()))
}

/**互斥操作*/
fun DslAdapter.selectMutex(dslItem: DslAdapterItem) {
    selector().selector(SelectorParams(dslItem, OPTION_MUTEX))
}



