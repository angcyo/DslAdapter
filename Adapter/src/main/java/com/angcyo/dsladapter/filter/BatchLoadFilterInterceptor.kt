package com.angcyo.dsladapter.filter

import android.os.Handler
import android.os.Looper
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslAdapterItem

/**
 * 数据批量处理加载拦截器
 * 比如:界面一下子Add了100个Item, 通过此拦截器.
 * 可以控制100个Item, 足步显示的界面上,而不是100个同时显示在界面上
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/04/16
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
open class BatchLoadFilterInterceptor : BaseFilterInterceptor(), Runnable {

    //控制延迟
    val mainHandle = Handler(Looper.getMainLooper())

    /**每次需要加载的数量*/
    var loadStepCount = 1

    /**每次加载延迟*/
    var loadDelay: Long = 16

    var _lastAdapter: DslAdapter? = null

    //当前加载到了什么位置
    var _lastLoadPosition = RecyclerView.NO_POSITION

    /**加载数量的差值器.[TimeInterpolator]*/
    var loadInterpolator: (loadPosition: Int, requestList: List<DslAdapterItem>) -> Int =
        { _, _ -> loadStepCount }

    override fun intercept(chain: FilterChain): List<DslAdapterItem> {
        val dslAdapter = chain.dslAdapter

        if (_lastAdapter != dslAdapter) {
            //更换了adapter, 重置加载位置.
            _lastLoadPosition = RecyclerView.NO_POSITION
            mainHandle.removeCallbacks(this)
        }

        _lastAdapter = dslAdapter

        val requestList = chain.requestList

        //数据量不够
        if (requestList.size <= _lastLoadPosition + 1 || dslAdapter.isAdapterStatus()) {
            return requestList
        }

        val count = if (_lastLoadPosition < 0) {
            loadInterpolator(_lastLoadPosition, requestList)
        } else {
            (_lastLoadPosition + 1) + loadInterpolator(_lastLoadPosition, requestList)
        }

        val result = ArrayList<DslAdapterItem>(count + count * 1 / 4)

        for (i in 0 until count) {
            result.add(requestList[i])
        }

        _lastLoadPosition = count - 1

        //数据量还有未加载完的
        if (requestList.size > _lastLoadPosition + 1) {
            mainHandle.postDelayed(this, loadDelay)
        }

        return result
    }

    override fun run() {
        //触发更新
        _lastAdapter?.apply {
            val rv = _recyclerView
            if (rv != null && ViewCompat.isAttachedToWindow(rv)) {
                updateItemDepend()
            }
        }
    }
}

/**快速配置[BatchLoadFilterInterceptor]*/
fun DslAdapter.batchLoad(
    delay: Long = 64,
    action: BatchLoadFilterInterceptor.() -> Unit = {}
): BatchLoadFilterInterceptor {
    return BatchLoadFilterInterceptor().apply {
        dslDataFilter?.afterFilterInterceptorList?.add(this)
        loadDelay = delay
        action()
    }
}