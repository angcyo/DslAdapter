package com.angcyo.dsladapter.data

import com.angcyo.dsladapter.*
import com.angcyo.dsladapter.annotation.UpdateByDiff

/**
 * [DslAdapter] 普通界面, 非列表界面的数据更新方式
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/20
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class SingleDataUpdate(val adapter: DslAdapter) {

    val opList = mutableListOf<Op>()

    fun remove(predicate: (index: Int, item: DslAdapterItem) -> Boolean) {
        adapter.adapterItems.forEachIndexed { index, dslAdapterItem ->
            if (predicate(index, dslAdapterItem)) {
                opList.add(Op(Op.REMOVE, dslAdapterItem))
            }
        }
    }

    inline fun <reified Item : DslAdapterItem> removeItem() {
        update { _, dslAdapterItem ->
            dslAdapterItem is Item
        }
    }

    fun add(item: DslAdapterItem, width: DslAdapterItem? = null) {
        opList.add(Op(Op.ADD, null, width, addItemList = listOf(item)))
    }

    fun add(itemList: List<DslAdapterItem>, width: DslAdapterItem? = null) {
        if (itemList.isNotEmpty()) {
            opList.add(Op(Op.ADD, null, width, addItemList = itemList))
        }
    }

    fun addWidth(predicate: (index: Int, item: DslAdapterItem) -> Boolean, item: DslAdapterItem) {
        adapter.adapterItems.forEachIndexed { index, dslAdapterItem ->
            if (predicate(index, dslAdapterItem)) {
                opList.add(Op(Op.ADD, null, dslAdapterItem, addItemList = listOf(item)))
            }
        }
    }

    /**需要更新的item
     * [predicate] 返回true, 表示当前的[DslAdapterItem]需要更新
     * */
    fun update(predicate: (index: Int, item: DslAdapterItem) -> Boolean) {
        adapter.adapterItems.forEachIndexed { index, dslAdapterItem ->
            if (predicate(index, dslAdapterItem)) {
                opList.add(Op(Op.UPDATE, dslAdapterItem))
            }
        }
    }

    /**
     * 通过类名, 快速匹配item进行更新
     * */
    inline fun <reified Item : DslAdapterItem> updateItem(crossinline init: Item.(index: Int) -> Unit) {
        update { index, dslAdapterItem ->
            if (dslAdapterItem is Item) {
                dslAdapterItem.init(index)
                true
            } else {
                false
            }
        }
    }

    /**开始批量更新item
     * [at] 在这个item后面开始批量更新相同类型的item, 不包含[at]
     * 如果[at]为空, 则在列表后面追加*/
    fun <Item : DslAdapterItem> updateListAt(
        itemClass: Class<Item>,
        at: DslAdapterItem?,
        dataList: List<Any?>?,
        initItem: Item.(data: Any?, dataIndex: Int) -> Unit = { _, _ -> }
    ) {

        //界面上已经存在的连续item
        val oldItemList = mutableListOf<DslAdapterItem>()
        var findAnchor = false
        if (at != null) {
            for (item in adapter.adapterItems) {
                if (item == at) {
                    findAnchor = true
                } else {
                    if (findAnchor) {
                        if (item.className() == itemClass.className()) {
                            oldItemList.add(item)
                        } else {
                            //不一样的item, 中断forEach
                            break
                        }
                    }
                }
            }
        }

        val updateStartIndex = 0
        val updateEndIndex = updateStartIndex + dataList.size()

        val newAddList = mutableListOf<DslAdapterItem>()

        //添加item操作时的锚点
        var addAnchorItem = at

        for (index in updateStartIndex until updateEndIndex) {

            val data = dataList?.getOrNull(index)
            val oldItem = oldItemList.getOrNull(index)

            val newItem = updateOrCreateItemByClass(itemClass, oldItem) {
                initItem(data, index)
            }

            /*if (newItem != null) {
                newItem.itemChanging = true
                newItem.itemData = data
            }*/
            if (oldItem == null) {
                if (newItem != null) {
                    //add item
                    newAddList.add(newItem)
                }
            } else {
                addAnchorItem = oldItem
                when {
                    //remove old item
                    newItem == null -> {
                        opList.add(Op(Op.REMOVE, oldItem))
                    }
                    //replace old item
                    oldItem != newItem -> {
                        opList.add(Op(Op.REPLACE, oldItem, newItem))
                    }
                    //update old item
                    oldItem == newItem -> {
                        opList.add(Op(Op.UPDATE, oldItem))
                    }
                }
            }
        }

        //超范围的旧数据
        for (i in updateEndIndex until oldItemList.size) {
            opList.add(Op(Op.REMOVE, oldItemList[i]))
        }

        if (newAddList.isNotEmpty()) {
            opList.add(Op(Op.ADD, null, addAnchorItem = addAnchorItem, addItemList = newAddList))
        }
    }

    /**开发派发更新*/
    fun doIt() {
        if (opList.isEmpty()) {
            return
        }

        //先处理add, 防止锚点不见
        opList.forEach { op ->
            val adapterItems = adapter.adapterItems
            when (op.op) {
                Op.ADD -> {
                    val addAnchorItem = op.addAnchorItem

                    if (addAnchorItem == null) {
                        adapterItems.addAll(op.addItemList ?: emptyList())
                    } else {
                        val index = adapterItems.indexOfFirst {
                            it == addAnchorItem
                        }
                        if (index != -1) {
                            adapterItems.addAll(index + 1, op.addItemList ?: emptyList())
                        }
                    }
                }
            }
        }

        val adapterItems = adapter.adapterItems
        //在处理其他
        opList.forEach { op ->
            when (op.op) {
                Op.REMOVE -> {
                    adapterItems.remove(op.item)
                }
                Op.UPDATE -> {
                    op.item?.itemUpdateFlag = true
                }
                Op.REPLACE -> {
                    val index = adapterItems.indexOfFirst {
                        it == op.item
                    }
                    if (index != -1) {
                        op.replaceItem?.let {
                            adapterItems.set(index, it)
                        }
                    }
                }
            }
        }

        //触发更新
        adapter.updateItemDepend()
    }

    data class Op(
        val op: Int,
        val item: DslAdapterItem?,
        val addAnchorItem: DslAdapterItem? = null,
        val replaceItem: DslAdapterItem? = null,
        val addItemList: List<DslAdapterItem>? = null
    ) {
        companion object {
            //无操作
            const val NO = 0b000000000

            //添加操作, 在锚点后面追加item list
            const val ADD = 0b00000001

            //移除操作
            const val REMOVE = 0b00010

            //更新操作
            const val UPDATE = 0b00100

            //替换操作
            const val REPLACE = 0b01000
        }
    }
}

@UpdateByDiff
fun DslAdapter.updateAdapter(update: SingleDataUpdate.() -> Unit) {
    SingleDataUpdate(this).apply {
        update()
        doIt()
    }
}

/**通过[itemClass]更新item*/
@UpdateByDiff
fun <Item : DslAdapterItem> DslAdapter.updateItem(
    itemClass: Class<Item>,
    initItem: Item.() -> Unit = { }
) {
    findItem(itemClass, true)?.apply {
        itemUpdateFlag = true
        (this as? Item)?.initItem()

        //触发更新
        updateItemDepend()
    }
}