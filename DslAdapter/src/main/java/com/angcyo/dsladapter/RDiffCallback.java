package com.angcyo.dsladapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.text.TextUtils;

import java.util.List;

public class RDiffCallback<T> extends DiffUtil.Callback {
    List<T> oldDatas;
    List<T> newDatas;
    RDiffCallback<T> mDiffCallback;

    public RDiffCallback() {
    }

    public RDiffCallback(List<T> oldDatas, List<T> newDatas, RDiffCallback<T> diffCallback) {
        this.oldDatas = oldDatas;
        this.newDatas = newDatas;
        mDiffCallback = diffCallback;
    }

    public static int getListSize(List list) {
        return list == null ? 0 : list.size();
    }

    @Override
    public int getOldListSize() {
        return getListSize(oldDatas);
    }

    @Override
    public int getNewListSize() {
        return getListSize(newDatas);
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return mDiffCallback.areItemsTheSame(oldDatas.get(oldItemPosition), newDatas.get(newItemPosition));
    }

    /**
     * 被DiffUtil调用，用来检查 两个item是否含有相同的数据
     * 这个方法仅仅在areItemsTheSame()返回true时，才调用。
     */
    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return mDiffCallback.areContentsTheSame(oldDatas.get(oldItemPosition), newDatas.get(newItemPosition));
    }

    /**
     * 重写此方法, 判断数据是否相等,
     * 如果item不相同, 会先调用 notifyItemRangeRemoved, 再调用 notifyItemRangeInserted
     */
    public boolean areItemsTheSame(@NonNull T oldData, @NonNull T newData) {
        Class<?> oldClass = oldData.getClass();
        Class<?> newClass = newData.getClass();
        if (oldClass.isAssignableFrom(newClass) || newClass.isAssignableFrom(oldClass)) {
            return true;
        }
        return TextUtils.equals(oldClass.getSimpleName(), newClass.getSimpleName());
    }

    /**
     * 重写此方法, 判断内容是否相等,
     * 如果内容不相等, 会调用notifyItemRangeChanged
     */
    public boolean areContentsTheSame(@NonNull T oldData, @NonNull T newData) {
        Class<?> oldClass = oldData.getClass();
        Class<?> newClass = newData.getClass();
        if (oldClass.isAssignableFrom(newClass) ||
                newClass.isAssignableFrom(oldClass) ||
                TextUtils.equals(oldClass.getSimpleName(), newClass.getSimpleName())) {
            return oldData.equals(newData);
        }
        return false;
    }
}