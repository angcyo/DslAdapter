package com.angcyo.dsladapter

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * 未定义的Drawable, 只做标识使用
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2020/03/23
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
class UndefinedDrawable : Drawable() {
    override fun draw(canvas: Canvas) {

    }

    override fun setAlpha(alpha: Int) {

    }

    override fun getOpacity(): Int = PixelFormat.OPAQUE

    override fun setColorFilter(colorFilter: ColorFilter?) {

    }
}