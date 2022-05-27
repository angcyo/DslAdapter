package com.angcyo.dsladapter.annotation

/**
 * 标识当前的方法/成员操作会触发Diff更新
 * [com.angcyo.dsladapter.DslAdapter.updateItemDepend]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class UpdateByDiff
