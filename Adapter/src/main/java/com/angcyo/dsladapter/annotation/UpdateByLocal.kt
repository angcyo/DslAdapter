package com.angcyo.dsladapter.annotation

/**
 * 直接获取[DslViewHolder]更新视图
 *
 * [com.angcyo.dsladapter.DslAdapterItemExKt.itemViewHolder]
 *
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/08/05
 */
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.SOURCE)
annotation class UpdateByLocal
