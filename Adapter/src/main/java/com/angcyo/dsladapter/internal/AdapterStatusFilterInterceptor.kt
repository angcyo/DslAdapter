package com.angcyo.dsladapter.internal

import com.angcyo.dsladapter.DslAdapterItem

/**
 *
 * 情感图状态拦截器
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/05
 */
class AdapterStatusFilterInterceptor : FilterInterceptor {
    override fun intercept(chain: FilterChain): List<DslAdapterItem> {
        return if (chain.dslAdapter.isAdapterStatus()) {
            chain.interruptChain = true
            mutableListOf(chain.dslAdapter.dslAdapterStatusItem)
        } else {
            chain.requestList
        }
    }
}