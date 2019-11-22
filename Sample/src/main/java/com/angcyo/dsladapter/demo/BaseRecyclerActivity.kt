package com.angcyo.dsladapter.demo

import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.angcyo.dsladapter.DslAdapter
import com.angcyo.dsladapter.DslItemDecoration
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.HoverItemDecoration
import com.angcyo.dsladapter.dsl.AppAdapterStatusItem
import kotlinx.android.synthetic.main.base_recycler_layout.*

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

    lateinit var refreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    lateinit var recyclerView: RecyclerView

    open fun initBaseLayout() {
        setSupportActionBar(toolbar)
        dslViewHolder.v<RecyclerView>(R.id.base_recycler_view)?.apply {
            recyclerView = this

            addItemDecoration(baseDslItemDecoration)
            hoverItemDecoration.attachToRecyclerView(this)

            //防止在折叠/展开 即 itemAdd/itemRemove 的时候, 自动滚动到顶部.
            //这个属性决定了, adapter 中的item 改变, 不会影响 RecyclerView 自身的宽高属性.
            //如果设置了true, 并且又想影响RecyclerView 自身的宽高属性. 调用 notifyDataSetChanged(),
            //否则统一 使用notifyItemXXX 变种方法
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = dslAdapter
        }

        dslViewHolder.v<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.base_refresh_layout)?.apply {
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
        dslAdapter.render()
    }
}