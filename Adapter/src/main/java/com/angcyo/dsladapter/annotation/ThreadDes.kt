package com.angcyo.dsladapter.annotation

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2022/10/28
 */

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class ThreadDes(val des: String = "" /*简单的描述*/)
