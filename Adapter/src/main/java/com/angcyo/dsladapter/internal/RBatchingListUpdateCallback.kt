package com.angcyo.dsladapter.internal

import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.BatchingListUpdateCallback
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.L
import com.angcyo.dsladapter.hash

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/20
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class RBatchingListUpdateCallback : BatchingListUpdateCallback {

    var log: Boolean = false

    constructor(callback: ListUpdateCallback) : super(callback)

    constructor(adapter: RecyclerView.Adapter<*>) : super(AdapterListUpdateCallback(adapter))

    override fun dispatchLastEvent() {
        super.dispatchLastEvent()
    }

    override fun onInserted(position: Int, count: Int) {
        super.onInserted(position, count)
        if (log) {
            L.v("${hash()} 插入列表从:${position} ${count}个")
        }
    }

    override fun onRemoved(position: Int, count: Int) {
        super.onRemoved(position, count)
        if (log) {
            L.v("${hash()} 移除列表从:${position} ${count}个")
        }
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        super.onMoved(fromPosition, toPosition)
        if (log) {
            L.v("${hash()} 移动列表从:${fromPosition} 到:${toPosition}")
        }
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        super.onChanged(position, count, payload)
        if (log) {
            L.v("${hash()} 改变列表:${position} ${count}个 payload:${payload}")
        }
    }
}