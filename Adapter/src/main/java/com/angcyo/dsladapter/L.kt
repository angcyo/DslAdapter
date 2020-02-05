package com.angcyo.dsladapter

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * 日志输出类
 * Email:angcyo@126.com
 * @author angcyo
 * @date 2019/12/30
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */

object L {
    fun isDebug() = BuildConfig.DEBUG

    val LINE_SEPARATOR = System.getProperty("line.separator")

    const val VERBOSE = 2
    const val DEBUG = 3
    const val INFO = 4
    const val WARN = 5
    const val ERROR = 6

    val DEFAULT_LOG_PRING: (tag: String, level: Int, msg: String) -> Unit =
        { tag, level, msg ->
            when (level) {
                VERBOSE -> Log.v(tag, msg)
                DEBUG -> Log.d(tag, msg)
                INFO -> Log.i(tag, msg)
                WARN -> Log.w(tag, msg)
                ERROR -> Log.e(tag, msg)
            }
        }

    var debug = isDebug()

    var tag: String = "L"
        get() {
            return _tempTag ?: field
        }

    /**打印多少级的堆栈信息*/
    var stackTraceDepth: Int = 2
        get() = if (_tempStackTraceDepth > 0) _tempStackTraceDepth else field

    var _tempStackTraceDepth: Int = -1

    /**堆栈跳过前多少个*/
    var stackTraceFront: Int = 2
        get() = if (_tempStackTraceFront > 0) _tempStackTraceFront else field

    var _tempStackTraceFront: Int = -1

    /**Json缩进偏移量*/
    var indentJsonDepth: Int = 2

    var logPrint: (tag: String, level: Int, msg: String) -> Unit = DEFAULT_LOG_PRING

    //临时tag
    var _tempTag: String? = null

    //当前日志输出级别
    var _level: Int = DEBUG

    fun init(tag: String, debug: Boolean = isDebug()) {
        this.tag = tag
        this.debug = debug
    }

    fun v(vararg msg: Any?) {
        _level = VERBOSE
        _log(*msg)
    }

    fun d(vararg msg: Any?) {
        _level = DEBUG
        _log(*msg)
    }

    fun i(vararg msg: Any?) {
        _level = INFO
        _log(*msg)
    }

    fun w(vararg msg: Any?) {
        _level = WARN
        _log(*msg)
    }

    fun e(vararg msg: Any?) {
        _level = ERROR
        _log(*msg)
    }

    fun vt(tag: String, vararg msg: Any?) {
        _tempTag = tag
        _level = VERBOSE
        _log(*msg)
    }

    fun dt(tag: String, vararg msg: Any?) {
        _tempTag = tag
        _level = DEBUG
        _log(*msg)
    }

    fun it(tag: String, vararg msg: Any?) {
        _tempTag = tag
        _level = INFO
        _log(*msg)
    }

    fun wt(tag: String, vararg msg: Any?) {
        _tempTag = tag
        _level = WARN
        _log(*msg)
    }

    fun et(tag: String, vararg msg: Any?) {
        _tempTag = tag
        _level = ERROR
        _log(*msg)
    }

    fun _log(vararg msg: Any?) {
        if (!debug) {
            return
        }

        val stackTrace = getStackTrace(stackTraceFront, stackTraceDepth)
        val stackContext = buildString {
            append("[")
            stackTrace.forEachIndexed { index, element ->
                append("(")
                append(element.fileName)
                if (index == stackTrace.lastIndex) {
                    append(":")
                    append(element.lineNumber)
                    append(")")
                }
                append("#")
                append(element.methodName)

                if (index == stackTrace.lastIndex) {
                    append(":")
                    append(Thread.currentThread().name)
                } else {
                    append("#")
                    append(element.lineNumber)
                    append(" ")
                }
            }
            append("]")
        }
        val logMsg = buildString {
            msg.forEach {
                if (it is CharSequence) {
                    append(_wrapJson("$it"))
                } else {
                    append(it)
                }
            }
        }

        logPrint(tag, _level, "$stackContext $logMsg")

        _tempTag = null
        _tempStackTraceDepth = -1
        _tempStackTraceFront = -1
    }

    fun _wrapJson(msg: String): String {
        if (indentJsonDepth <= 0) {
            return msg
        }
        try {
            if (msg.startsWith("{") && msg.endsWith("}")) {
                val jsonObject = JSONObject(msg)
                return LINE_SEPARATOR + jsonObject.toString(indentJsonDepth)
            } else if (msg.startsWith("[") && msg.endsWith("]")) {
                val jsonArray = JSONArray(msg)
                return LINE_SEPARATOR + jsonArray.toString(indentJsonDepth)
            }
        } catch (e: Exception) {

        }
        return msg
    }
}

/**
 * 获取调用栈信息
 * [front] 当前调用位置的前几个开始
 * [count] 共几个, 负数表示全部
 * */
fun getStackTrace(front: Int = 0, count: Int = -1): List<StackTraceElement> {
    val stackTrace = Thread.currentThread().stackTrace
    stackTrace.reverse()
    val endIndex = stackTrace.size - 3 - front
    val startIndex = if (count > 0) (endIndex - count) else 0
    val slice = stackTrace.slice(startIndex until endIndex)
    return slice
}
