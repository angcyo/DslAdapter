package com.angcyo.dsladapter.internal

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.filter.BaseFilterInterceptor
import com.angcyo.dsladapter.filter.FilterChain

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/05
 */

class SubItemFilterInterceptor : BaseFilterInterceptor() {

    /**过滤需要追加或者隐藏的子项*/
    override fun intercept(chain: FilterChain): List<DslAdapterItem> {
        val result = mutableListOf<DslAdapterItem>()

        chain.requestList.forEach { currentItem ->
            val parentList = mutableListOf<DslAdapterItem>()
            val subList = mutableListOf<DslAdapterItem>()
            loadSubItemList(currentItem, parentList, subList)
            result.addAll(subList)
        }

        return result
    }

    /**枚举加载所有子项*/
    private fun loadSubItemList(
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
                currentItem.itemLoadSubList()
                currentItem.itemSubList.forEach {
                    val pList = ArrayList(parentList)
                    pList.add(currentItem)
                    loadSubItemList(it, pList, subList)
                }
            }
        }
    }
}