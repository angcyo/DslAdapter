package com.angcyo.dsladapter.internal

import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.filter.BaseFilterInterceptor
import com.angcyo.dsladapter.filter.FilterChain

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/05
 */
class GroupItemFilterInterceptor : BaseFilterInterceptor() {

    /**过滤折叠后后的数据列表*/
    override fun intercept(chain: FilterChain): List<DslAdapterItem> {
        val dslAdapter = chain.dslAdapter
        val requestList = chain.requestList

        val list = mutableListOf<DslAdapterItem>()

        var index = 0

        while (index < requestList.size) {
            val item = requestList[index]

            //第一条数据, 要么是分组头, 要么是 不受分组管理的子项
            list.add(item)

            if (item.itemIsGroupHead) {
                val childSize = groupChildSize(dslAdapter, requestList, index)
                index += 1

                if (childSize > 0) {
                    if (item.itemGroupExtend && !item.itemHidden) {
                        //本身不隐藏的情况下展开状态
                        for (i in index..(index - 1 + childSize)) {
                            list.add(requestList[i])
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

    /*当前位置, 距离下一个分组头, 还有多少个数据 (startIndex, endIndex)*/
    private fun groupChildSize(
        dslAdapter: DslAdapter,
        originList: List<DslAdapterItem>,
        startIndex: Int
    ): Int {
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
}