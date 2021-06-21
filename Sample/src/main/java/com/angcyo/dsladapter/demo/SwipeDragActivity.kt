package com.angcyo.dsladapter.demo

import com.angcyo.dsladapter.DragCallbackHelper

/**
 *
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/06/21
 * Copyright (c) 2020 ShenZhen Wayto Ltd. All rights reserved.
 */
class SwipeDragActivity : SwipeMenuActivity() {

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        DragCallbackHelper.install(dslViewHolder.v(R.id.base_recycler_view)!!).apply {
            swipeMenuHelper?._dragCallbackHelper = this
        }
    }
}