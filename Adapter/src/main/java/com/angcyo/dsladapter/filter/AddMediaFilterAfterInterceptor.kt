package com.angcyo.dsladapter.filter

import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.toNone

/**
 * 当数量不足时, 显示媒体添加item
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/25
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
open class AddMediaFilterAfterInterceptor : BaseFilterAfterInterceptor() {

    /**显示的最大媒体数, 不足时. 追加[addMediaDslAdapterItem]*/
    var maxMediaCount = Int.MAX_VALUE

    var addMediaDslAdapterItem: DslAdapterItem? = null

    /**激活[adapter]状态切换*/
    var enableChangeAdapterState: Boolean = true

    override fun intercept(chain: FilterAfterChain): List<DslAdapterItem> {
        if (chain.requestList.size >= maxMediaCount) {
            return chain.requestList.subList(0, maxMediaCount)
        }

        addMediaDslAdapterItem?.apply {

            if (enableChangeAdapterState) {
                chain.dslAdapter.toNone()
            }

            val result = mutableListOf<DslAdapterItem>()
            result.addAll(chain.requestList)
            result.add(this)
            return result
        }

        return chain.requestList
    }
}