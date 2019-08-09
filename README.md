# DslAdapter
Dsl 的形式使用 RecyclerView.Adapter, 支持情感图状态切换, 加载更多, 多类型Item等

# 特性
- 1.全网最轻量的多类型Item实现方法
- 2.支持情感图状态(空布局 加载中 错误异常等)切换 完美支持自定义扩展
- 3.支持加载更多 完美支持自定义扩展

# 使用
# 1. 多类型使用方式

## 1.1 方式1(推荐)

继承 `DslAdapterItem`  重写 `itemLayoutId`  实现 `itemBind` 搞定.

```kotlin
class DslTextItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_text_layout
    }

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, itemPosition, _ ->
            itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
        }
}
```

## 1.2 方式2

调用扩展方法`dslItem` 传入`layoutId`参数 实现 `itemBind` 搞定.

```kotlin
dslAdapter.dslItem(R.layout.item_text_layout) {
          itemBind = { itemHolder, itemPosition, _ ->
              itemHolder.v<TextView>(R.id.text_view).text = "文本位置:$itemPosition"
          }
      }
```

# 2. 情感图自定义

继承`DslAdapterStatusItem` 将对象设置给`DslAdapter`的`dslAdapterStatusItem`变量 即可.

```kotlin
open class DslAdapterStatusItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_adapter_status
    }

    companion object {
        const val ADAPTER_STATUS_NONE = -1
        const val ADAPTER_STATUS_EMPTY = 0
        const val ADAPTER_STATUS_LOADING = 1
        const val ADAPTER_STATUS_ERROR = 2
    }

    var itemAdapterStatus: Int = ADAPTER_STATUS_NONE

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, _, _ ->

            /*具体逻辑, 自行处理*/
            itemHolder.v<TextView>(R.id.text_view).text = "情感图状态: ${when (itemAdapterStatus) {
                ADAPTER_STATUS_EMPTY -> "空数据"
                ADAPTER_STATUS_LOADING -> "加载中"
                ADAPTER_STATUS_ERROR -> "加载异常"
                else -> "未知状态"
            }}"
        }

    /**返回[true] 表示不需要显示情感图, 即显示[Adapter]原本的内容*/
    open fun isNoStatus() = itemAdapterStatus == ADAPTER_STATUS_NONE
}

```

# 3. 加载更多自定义
继承`DslLoadMoreItem` 将对象设置给`DslAdapter`的`dslLoadMoreItem`变量 即可.

```kotlin
open class DslLoadMoreItem : DslAdapterItem() {
    init {
        itemLayoutId = R.layout.item_load_more
    }

    companion object {
        const val ADAPTER_LOAD_NORMAL = 0
        const val ADAPTER_LOAD_LOADING = 1
        const val ADAPTER_LOAD_ERROR = 2
        const val ADAPTER_LOAD_NO_MORE = 3
    }

    /**是否激活加载更多*/
    var itemEnableLoadMore = true
        set(value) {
            field = value
            itemLoadMoreStatus = ADAPTER_LOAD_NORMAL
        }

    /**加载更多当前的状态*/
    var itemLoadMoreStatus: Int = ADAPTER_LOAD_NORMAL

    /**加载更多回调*/
    var onLoadMore: (DslViewHolder) -> Unit = {}

    override var itemBind: (itemHolder: DslViewHolder, itemPosition: Int, adapterItem: DslAdapterItem) -> Unit =
        { itemHolder, _, _ ->

            /*具体逻辑, 自行处理*/
            itemHolder.v<TextView>(R.id.text_view).text = "加载更多: ${when (itemLoadMoreStatus) {
                ADAPTER_LOAD_NORMAL -> "加载更多中..."
                ADAPTER_LOAD_LOADING -> "加载更多中..."
                ADAPTER_LOAD_ERROR -> "加载异常"
                ADAPTER_LOAD_NO_MORE -> "我是有底线的"
                else -> "未知状态"
            }}"

            if (itemEnableLoadMore) {
                if (itemLoadMoreStatus == ADAPTER_LOAD_NORMAL) {
                    //错误和正常的情况下, 才触发加载跟多
                    itemLoadMoreStatus = ADAPTER_LOAD_LOADING
                    onLoadMore(itemHolder)
                }
            }
        }

    override var onItemViewDetachedToWindow: (itemHolder: DslViewHolder) -> Unit = {
        if (itemEnableLoadMore) {
            //加载失败时, 下次是否还需要加载更多?
            if (itemLoadMoreStatus == ADAPTER_LOAD_ERROR) {
                itemLoadMoreStatus = ADAPTER_LOAD_NORMAL
            }
        }
    }
}

```
