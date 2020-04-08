package com.angcyo.dsladapter.internal

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.filter.BaseFilterInterceptor
import com.angcyo.dsladapter.filter.FilterChain

/**
 *
 * 加载更多数据拦截器
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/05
 */
class LoadMoreFilterInterceptor : BaseFilterInterceptor() {
    override fun intercept(chain: FilterChain): List<DslAdapterItem> {
        return if (chain.dslAdapter.isAdapterStatus() ||
            !chain.dslAdapter.dslLoadMoreItem.itemStateEnable
        ) {
            chain.requestList
        } else {
            val result = mutableListOf<DslAdapterItem>()
            result.addAll(chain.requestList)
            result.add(chain.dslAdapter.dslLoadMoreItem)
            result
        }
    }
}