package com.angcyo.dsladapter.annotation

/**
 * 标识当前的方法/成员操作会触发notify更新
 * [androidx.recyclerview.widget.RecyclerView.Adapter.notifyItemChanged]
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/05/27
 */

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY
)
@Retention(AnnotationRetention.SOURCE)
annotation class UpdateByNotify
