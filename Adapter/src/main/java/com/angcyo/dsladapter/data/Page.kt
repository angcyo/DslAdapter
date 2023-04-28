package com.angcyo.dsladapter.data

import androidx.annotation.Keep

/**
 * 网络请求, 页面操作参数
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */

@Keep
open class Page {
    companion object {

        /**默认一页请求的数量*/
        var PAGE_SIZE: Int = 20

        /**默认第一页的索引*/
        var FIRST_PAGE_INDEX: Int = 1

        /**请求的Key*/
        var KEY_CURRENT = "current"

        var KEY_SIZE = "size"
    }

    /**默认的第一页*/
    var firstPageIndex: Int = FIRST_PAGE_INDEX
        set(value) {
            field = value
            pageRefresh()
        }

    /** 当前请求完成的页 */
    var _currentPageIndex: Int = firstPageIndex

    /** 正在请求的页 */
    var requestPageIndex: Int = firstPageIndex

    /** 每页请求的数量 */
    var requestPageSize: Int = PAGE_SIZE

    /**当前请求开始的索引*/
    val currentStartIndex: Int
        get() = (requestPageIndex - firstPageIndex) * requestPageSize

    /**当前请求结束的索引*/
    val currentEndIndex: Int
        get() = currentStartIndex + requestPageSize

    /**页面刷新, 重置page index*/
    open fun pageRefresh() {
        _currentPageIndex = firstPageIndex
        requestPageIndex = firstPageIndex
    }

    /**页面加载更多*/
    open fun pageLoadMore() {
        requestPageIndex = _currentPageIndex + 1
    }

    /**页面加载结束, 刷新结束/加载更多结束*/
    open fun pageLoadEnd() {
        _currentPageIndex = requestPageIndex
    }

    /**重新赋值*/
    open fun set(page: Page) {
        firstPageIndex = page.firstPageIndex
        _currentPageIndex = page._currentPageIndex
        requestPageIndex = page.requestPageIndex
        requestPageSize = page.requestPageSize
    }

    /**是否是第一页请求*/
    open fun isFirstPage() = requestPageIndex == firstPageIndex

    /**单列表数据, 无加载更多*/
    open fun singlePage() {
        requestPageSize = Int.MAX_VALUE
    }

    /**需要降序排序字段(从大->小), 多个用;分割*/
    var desc: String? = null

    /**需要升序排序字段(从小->大), 多个用;分割*/
    var asc: String? = null

    /**请求相关的2个key*/
    var keyCurrent: String = KEY_CURRENT

    var keySize: String = KEY_SIZE
}

/**单页请求, 无加载更多*/
fun singlePage() = Page().apply {
    requestPageSize = Int.MAX_VALUE
}