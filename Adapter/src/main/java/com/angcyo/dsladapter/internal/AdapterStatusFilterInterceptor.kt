package com.angcyo.dsladapter.internal

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.filter.BaseFilterInterceptor
import com.angcyo.dsladapter.filter.FilterChain

/**
 *
 * 情感图状态拦截器
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/05
 */
class AdapterStatusFilterInterceptor : BaseFilterInterceptor() {
    override fun intercept(chain: FilterChain): List<DslAdapterItem> {
        return if (chain.dslAdapter.isAdapterStatus()) {
            chain.interruptChain = true
            mutableListOf(chain.dslAdapter.dslAdapterStatusItem)
        } else {
            chain.requestList
        }
    }
}