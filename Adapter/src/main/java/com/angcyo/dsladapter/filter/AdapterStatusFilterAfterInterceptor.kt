package com.angcyo.dsladapter.filter

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslAdapterStatusItem

/**
 * 当Diff计算后, 数据为空. 则切换到空视图
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/06
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

class AdapterStatusFilterAfterInterceptor : BaseFilterAfterInterceptor() {

    override fun intercept(chain: FilterAfterChain): List<DslAdapterItem> {
        if (chain.requestList.isEmpty() && chain.originList.isNotEmpty()) {
            chain.interruptChain = true

            //切换到空布局
            chain.dslAdapter.apply {
                _recyclerView?.post {
                    updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
                }
            }
        }
        return chain.requestList
    }
}