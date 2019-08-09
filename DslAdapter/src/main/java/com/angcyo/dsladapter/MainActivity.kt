package com.angcyo.dsladapter

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import com.angcyo.dsladapter.dsl.*

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    lateinit var dslViewHolder: DslViewHolder
    var dslAdapter: DslAdapter = DslAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        dslViewHolder = DslViewHolder(window.decorView)
        initLayout()
        initAdapterStatus()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initLayout() {
        dslViewHolder.v<SwipeRefreshLayout>(R.id.refresh_layout).apply {
            setOnRefreshListener {
                Toast.makeText(this@MainActivity, "刷新", Toast.LENGTH_SHORT).show()
                postDelayed({
                    isRefreshing = false
                }, 1000)
            }
        }

        dslViewHolder.v<RecyclerView>(R.id.recycler_view).apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = dslAdapter
        }

        for (i in 0..10) {
            //2种使用item的方式, 喜欢哪种方式, 就用哪一种
            dslAdapter.dslTextItem()
            dslAdapter.dslItem(R.layout.item_text_layout) {
                itemBind = { itemHolder, itemPosition, _ ->
                    itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
                }
            }

            for (j in 0..0) {
                //2种使用item的方式, 喜欢哪种方式, 就用哪一种
                dslAdapter.dslImageItem()
                dslAdapter.dslItem(R.layout.item_image_layout) {
                    itemBind = { itemHolder, itemPosition, _ ->
                        itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
                    }
                }
            }
        }
    }

    private fun initAdapterStatus() {
        dslViewHolder.click(R.id.normal) {
            dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
        }
        dslViewHolder.click(R.id.empty) {
            dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        }
        dslViewHolder.click(R.id.loading) {
            dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }
        dslViewHolder.click(R.id.error) {
            dslAdapter.setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_ERROR)
        }
    }
}
