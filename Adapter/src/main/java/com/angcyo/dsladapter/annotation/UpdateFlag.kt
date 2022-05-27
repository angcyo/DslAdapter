package com.angcyo.dsladapter.annotation

/**
 * 标识当前的方法/成员操作只是一个更新标识, 并不会触发界面刷新.
 * 此时需要主动调用界面更新才能生效
 * [com.angcyo.dsladapter.DslAdapter.updateItemDepend]
 * [com.angcyo.dsladapter.DslAdapter.notifyDataChanged]
 * [com.angcyo.dsladapter.DslAdapter.updateAllItem]
 * [com.angcyo.dsladapter.DslAdapter.render]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class UpdateFlag
