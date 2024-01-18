package com.angcyo.dsladapter.demo

import android.view.View
import android.view.ViewGroup
import com.angcyo.dsladapter.DslAdapterItem
import com.angcyo.dsladapter.DslViewHolder
import com.angcyo.dsladapter.dpi

/**
 * @author <a href="mailto:angcyo@126.com">angcyo</a>
 * @since 2024/01/18
 */
class TreeSelectDemoActivity : BaseRecyclerActivity() {

    override fun onInitBaseLayoutAfter() {
        super.onInitBaseLayoutAfter()

        val beanList = mutableListOf<TreeSelectBean>()
        beanList.add(TreeSelectBean(text = "第一组").apply {
            childList = mutableListOf<TreeSelectBean>().apply {
                add(TreeSelectBean(level = level + 1, text = "第一组-1").apply {
                    childList = mutableListOf<TreeSelectBean>().apply {
                        add(TreeSelectBean(level = level + 1, text = "第一组-1-1"))
                        add(TreeSelectBean(level = level + 1, text = "第一组-1-2"))
                        add(TreeSelectBean(level = level + 1, text = "第一组-1-3"))
                    }
                })
                add(TreeSelectBean(level = level + 1, text = "第一组-2"))
                add(TreeSelectBean(level = level + 1, text = "第一组-3"))
            }
        })
        beanList.add(TreeSelectBean(text = "第二组").apply {
            childList = mutableListOf<TreeSelectBean>().apply {
                add(TreeSelectBean(level = level + 1, text = "第二组-1").apply {
                    childList = mutableListOf<TreeSelectBean>().apply {
                        add(TreeSelectBean(level = level + 1, text = "第二组-1-1"))
                        add(TreeSelectBean(level = level + 1, text = "第二组-1-2"))
                        add(TreeSelectBean(level = level + 1, text = "第二组-1-3"))
                    }
                })
                add(TreeSelectBean(level = level + 1, text = "第二组-2"))
                add(TreeSelectBean(level = level + 1, text = "第二组-3"))
            }
        })

        renderAdapter {
            for (bean in beanList) {
                TreeSelectItem()() {
                    itemBean = bean
                }
            }
        }
    }
}

data class TreeSelectBean(
    /**分级信息*/
    var level: Int = 0,
    /**显示的文本内容*/
    var text: CharSequence? = null,
    /**选中状态
     * -1: 未选中
     * 0: 半选中
     * 1: 选中
     * */
    var selectType: Int = -1,
    /**是否展开*/
    var isExpand: Boolean = false,
    /**子项*/
    var childList: MutableList<TreeSelectBean>? = null
)

class TreeSelectItem : DslAdapterItem() {

    var itemBean: TreeSelectBean? = null
        set(value) {
            field = value
            itemData = value

            itemGroupExtend = value?.isExpand ?: false

            itemSubList = value?.childList?.mapTo(mutableListOf()) {
                TreeSelectItem().apply {
                    itemBean = it
                }
            } ?: mutableListOf()
        }

    init {
        itemLayoutId = R.layout.item_tree_select_layout
    }

    override fun onItemBind(
        itemHolder: DslViewHolder,
        itemPosition: Int,
        adapterItem: DslAdapterItem,
        payloads: List<Any>
    ) {
        itemHolder.tv(R.id.text_view)?.text = itemBean?.text
        val isLastItem = (itemBean?.level ?: Int.MAX_VALUE) >= 2

        itemHolder.img(R.id.check_view)?.setImageResource(
            when (itemBean?.selectType) {
                -1 -> R.drawable.ic_check_normal
                1 -> R.drawable.ic_checked
                else -> R.drawable.ic_check_mid
            }
        )
        //3级后, 不显示icon
        itemHolder.invisible(
            R.id.icon_view,
            isLastItem /*|| itemSubList.isEmpty()*/
        )

        //---

        //展开or关闭动画控制
        itemHolder.clickItem {
            itemHolder.v<View>(R.id.icon_view)?.run {
                animate()
                    .setDuration(300)
                    .rotation(if (itemGroupExtend) 0f else 90f)
                    .start()
            }
            itemGroupExtend = !itemGroupExtend
            itemBean?.isExpand = itemGroupExtend
        }

        //缩进控制
        itemHolder.v<View>(R.id.icon_view)?.apply {
            (layoutParams as? ViewGroup.MarginLayoutParams)?.run {
                leftMargin = itemParentList.size * 20 * dpi
                layoutParams = this
            }
        }

        //选中控制
        itemHolder.click(R.id.check_view) {
            if (isLastItem) {
                //第3级的选中状态, 只能是选中和未选中
                itemBean?.selectType = when (itemBean?.selectType) {
                    -1 -> 1
                    else -> -1
                }
                updateAdapterItem()
            } else {
                when (itemBean?.selectType) {
                    -1 -> 1
                    else -> -1
                }.apply {
                    itemBean?.selectType = this
                    selectAllChildType(this)
                }
                itemDslAdapter?.updateAllItem()
            }
            //更新父级选中状态
            for (parentItem in itemParentList) {
                if (parentItem is TreeSelectItem) {
                    updateParentSelectType(parentItem.itemBean)
                    parentItem.updateAdapterItem()
                }
            }
        }
    }

    /**选中所有child*/
    private fun selectAllChildType(selectType: Int = 1, bean: TreeSelectBean? = itemBean) {
        bean?.childList?.forEach {
            it.selectType = selectType
            selectAllChildType(selectType, it)
        }
    }

    /**更新parent的选中状态*/
    private fun updateParentSelectType(parent: TreeSelectBean?) {
        parent?.let {
            if (it.childList.isNullOrEmpty()) {
                //没有child, 不需要更新
                return
            }

            val selectType = when {
                isAllChildSelectType(1, it) -> 1
                isAllChildSelectType(-1, it) -> -1
                else -> 0
            }

            it.selectType = selectType
        }
    }

    /**所有child是否选中*/
    private fun isAllChildSelectType(
        selectType: Int = 1,
        bean: TreeSelectBean? = itemBean
    ): Boolean {
        bean?.childList?.forEach {
            if (it.selectType != selectType) {
                return false
            }
            if (!isAllChildSelectType(selectType, it)) {
                return false
            }
        }
        return true
    }
}