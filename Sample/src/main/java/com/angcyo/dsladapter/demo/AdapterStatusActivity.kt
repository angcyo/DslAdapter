package com.angcyo.dsladapter.demo

import com.angcyo.dsladapter.DslAdapterStatusItem

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/10/16
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class AdapterStatusActivity : BaseRecyclerActivity() {

    override fun getBaseLayoutId(): Int {
        return R.layout.activity_adatper_startus
    }

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        dslAdapter.render {
            setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }

        dslViewHolder.postDelay(1000) {
            dslAdapter.render {
                setAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
                来点数据()
            }
        }

        initAdapterStatus()
    }

    private fun initAdapterStatus() {
        dslViewHolder.click(R.id.normal) {
            dslAdapter.updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_NONE)
        }
        dslViewHolder.click(R.id.empty) {
            dslAdapter.updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_EMPTY)
        }
        dslViewHolder.click(R.id.loading) {
            dslAdapter.updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_LOADING)
        }
        dslViewHolder.click(R.id.error) {
            dslAdapter.updateAdapterStatus(DslAdapterStatusItem.ADAPTER_STATUS_ERROR)
        }
    }

    override fun onRefresh() {
        super.onRefresh()

        dslAdapter.render {
            resetItem(listOf())
            来点数据()
        }
    }
}
