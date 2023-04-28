package com.angcyo.dsladapter.demo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslItemDecoration
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.HoverItemDecoration
import com.angcyo.dsladapter.dsl.AppAdapterStatusItem

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
abstract class BaseRecyclerActivity : AppCompatActivity() {

    lateinit var dslViewHolder: DslViewHolder

    /**提供悬停功能*/
    var hoverItemDecoration = HoverItemDecoration()

    /**提供基本的分割线功能*/
    var baseDslItemDecoration = DslItemDecoration()

    var dslAdapter: DslAdapter = DslAdapter().apply {
        dslAdapterStatusItem = AppAdapterStatusItem()
    }

    open fun getBaseLayoutId(): Int = R.layout.base_recycler_layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getBaseLayoutId())
        dslViewHolder = DslViewHolder(window.decorView)
        initBaseLayout()
        onInitBaseLayoutAfter()
    }

    lateinit var refreshLayout: SwipeRefreshLayout
    lateinit var recyclerView: RecyclerView

    open fun initBaseLayout() {
        setSupportActionBar(dslViewHolder.v(R.id.toolbar))
        dslViewHolder.v<RecyclerView>(R.id.base_recycler_view)?.apply {
            recyclerView = this

            addItemDecoration(baseDslItemDecoration)
            hoverItemDecoration.attachToRecyclerView(this)

            layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
                override fun onLayoutChildren(
                    recycler: RecyclerView.Recycler?,
                    state: RecyclerView.State?
                ) {
                    try {
                        super.onLayoutChildren(recycler, state)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            adapter = dslAdapter
        }

        dslViewHolder.v<SwipeRefreshLayout>(R.id.base_refresh_layout)
            ?.apply {
                refreshLayout = this
                setOnRefreshListener {
                    onRefresh()
                }
            }
    }

    open fun onRefresh() {
        Toast.makeText(this, "刷新", Toast.LENGTH_SHORT).show()
        dslViewHolder.postDelay(1000) {
            refreshLayout.isRefreshing = false
        }
    }

    open fun onInitBaseLayoutAfter() {

    }

    open fun renderAdapter(render: DslAdapter.() -> Unit) {
        dslAdapter.render(action = render)
    }
}