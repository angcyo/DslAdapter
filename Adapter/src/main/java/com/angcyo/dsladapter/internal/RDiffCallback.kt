package com.angcyo.dsladapter.internal

import android.text.TextUtils
import androidx.recyclerview.widget.DiffUtil

open class RDiffCallback<T : Any>(
    val oldDataList: List<T>,
    val newDataList: List<T>,
    val itemDiffCallback: RItemDiffCallback<T>? = null
) : DiffUtil.Callback() {

    companion object {
        fun getListSize(list: List<*>?): Int {
            return list?.size ?: 0
        }
    }

    override fun getOldListSize(): Int {
        return getListSize(
            oldDataList
        )
    }

    override fun getNewListSize(): Int {
        return getListSize(
            newDataList
        )
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return itemDiffCallback?.getChangePayload(
            oldDataList[oldItemPosition],
            newDataList[newItemPosition],
            oldItemPosition, newItemPosition
        )
    }

    override fun areItemsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        return itemDiffCallback?.areItemsTheSame(
            oldDataList[oldItemPosition],
            newDataList[newItemPosition],
            oldItemPosition, newItemPosition
        ) ?: false
    }

    /**
     * 被DiffUtil调用，用来检查 两个item是否含有相同的数据
     * 这个方法仅仅在areItemsTheSame()返回true时，才调用。
     */
    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        return itemDiffCallback?.areContentsTheSame(
            oldDataList[oldItemPosition],
            newDataList[newItemPosition],
            oldItemPosition, newItemPosition
        ) ?: false
    }
}

interface RItemDiffCallback<T : Any> {
    fun getChangePayload(oldData: T, newData: T, oldItemPosition: Int, newItemPosition: Int): Any? {
        return null
    }

    /**
     * 重写此方法, 判断数据是否相等,
     * 如果item不相同, 会先调用 notifyItemRangeRemoved, 再调用 notifyItemRangeInserted
     */
    fun areItemsTheSame(
        oldData: T,
        newData: T,
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        val oldClass: Class<*> = oldData.javaClass
        val newClass: Class<*> = newData.javaClass
        return TextUtils.equals(
            oldClass.simpleName,
            newClass.simpleName
        ) && oldItemPosition == newItemPosition
    }

    /**
     * 重写此方法, 判断内容是否相等,
     * 如果内容不相等, 会调用notifyItemRangeChanged
     */
    fun areContentsTheSame(
        oldData: T,
        newData: T,
        oldItemPosition: Int,
        newItemPosition: Int
    ): Boolean {
        val oldClass: Class<*> = oldData.javaClass
        val newClass: Class<*> = newData.javaClass
        return if (oldClass.isAssignableFrom(newClass) ||
            newClass.isAssignableFrom(oldClass) ||
            TextUtils.equals(oldClass.simpleName, newClass.simpleName)
        ) {
            oldData == newData
        } else false
    }
}