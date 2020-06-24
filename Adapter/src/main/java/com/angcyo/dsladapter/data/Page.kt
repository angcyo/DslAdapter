package com.angcyo.dsladapter.data

/**
 * 网络请求, 页面操作参数
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/03
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class Page {
    companion object {
        /**默认一页请求的数量*/
        var PAGE_SIZE: Int = 20

        /**默认第一页的索引*/
        var FIRST_PAGE_INDEX: Int = 1
    }

    /**默认的第一页*/
    var firstPageIndex: Int =
        FIRST_PAGE_INDEX
        set(value) {
            field = value
            pageRefresh()
        }

    /** 当前请求完成的页 */
    var _currentPageIndex: Int = firstPageIndex

    /** 正在请求的页 */
    var requestPageIndex: Int = firstPageIndex

    /** 每页请求的数量 */
    var requestPageSize: Int =
        PAGE_SIZE

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
}

/**单页请求, 无加载更多*/
fun singlePage() = Page().apply {
    requestPageSize = Int.MAX_VALUE
}