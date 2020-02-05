package com.angcyo.dsladapter.internal

import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.FilterParams

/**
 * 数据过滤拦截器
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/02/05
 */
interface FilterInterceptor {

    /**数据源过滤拦截*/
    fun intercept(
        dslAdapter: DslAdapter,
        filterParams: FilterParams,
        requestList: List<DslAdapterItem>
    ): MutableList<DslAdapterItem>
}