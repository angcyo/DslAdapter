package com.angcyo.dsladapter.item

import android.view.View
import androidx.annotation.IdRes
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.containsPayload

/**
 * 操作[com.angcyo.dsladapter.DslAdapterItem.itemGroupExtend]
 * 支持箭头动画
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2021/07/01
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
interface IExtendItem : IDslItem {

    fun initExtendItem(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>,
        @IdRes arrowViewId: Int,
        @IdRes triggerViewId: Int = View.NO_ID, /*触发旋转控件的id, 默认是itemView*/
        extendRotation: Float = 0f, /*展开时的旋转角度*/
        foldRotation: Float = 180f, /*折叠时的旋转角度*/
        toExtendRotation: Float = 180f, /*动画到展开需要旋转的角度*/
        toFoldRotation: Float = 180f, /*动画到折叠需要旋转的角度*/
    ) {

        val extend = adapterItem.itemGroupExtend

        //初始化
        if (!payloads.containsPayload(DslAdapterItem.PAYLOAD_UPDATE_EXTEND)) {
            //负载时, 不更新. 因为动画已在更新角度.
            itemHolder.view(arrowViewId)?.rotation = if (extend) extendRotation else foldRotation
        }

        fun trigger() {
            itemHolder.view(arrowViewId)?.animate()?.apply {
                if (extend) {
                    //to fold
                    rotationBy(toFoldRotation)
                } else {
                    //to extend
                    rotationBy(toExtendRotation)
                }
                duration = 300
                start()
            }
            adapterItem.itemGroupExtend = !extend
        }

        //触发
        if (triggerViewId == View.NO_ID) {
            itemHolder.clickItem {
                trigger()
            }
        } else {
            itemHolder.click(triggerViewId) {
                trigger()
            }
        }
    }

}