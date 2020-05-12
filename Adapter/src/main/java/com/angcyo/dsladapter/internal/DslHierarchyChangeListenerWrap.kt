package com.angcyo.dsladapter.internal

import android.view.View
import android.view.ViewGroup
import com.angcyo.dsladapter.tagDslAdapterItem
import com.angcyo.dsladapter.tagDslViewHolder

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/05
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class DslHierarchyChangeListenerWrap(var origin: ViewGroup.OnHierarchyChangeListener? = null) :
    ViewGroup.OnHierarchyChangeListener {

    fun View?.childIndex(child: View?): Int {
        if (child == null) {
            return -1
        }

        return if (this is ViewGroup) {
            indexOfChild(child)
        } else {
            -1
        }
    }

    override fun onChildViewRemoved(parent: View?, child: View?) {
        origin?.onChildViewRemoved(parent, child)
        child?.tagDslAdapterItem()?.run {
            child.tagDslViewHolder()?.also { dslViewHolder ->
                itemViewDetachedToWindow(dslViewHolder, parent.childIndex(child))
            }
        }
    }

    override fun onChildViewAdded(parent: View?, child: View?) {
        origin?.onChildViewAdded(parent, child)
        child?.tagDslAdapterItem()?.run {
            child.tagDslViewHolder()?.also { dslViewHolder ->
                itemViewAttachedToWindow(dslViewHolder, parent.childIndex(child))
            }
        }
    }
}