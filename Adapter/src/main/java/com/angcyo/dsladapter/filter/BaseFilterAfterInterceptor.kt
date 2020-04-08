package com.angcyo.dsladapter.filter

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/08
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
abstract class BaseFilterAfterInterceptor : IFilterAfterInterceptor {
    override var isEnable: Boolean = true
}