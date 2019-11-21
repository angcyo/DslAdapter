package com.angcyo.dsladapter;

import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.lang.ref.WeakReference;

/**
 * 为什么这个类, 要用java代码?
 * <p>
 * 因为 使用java代码, 如果没有使用 [@Nullable] 注解,
 * 那么 在kotlin代码里面, 你既可以用?, 也可以不用?.
 * IDE 还不会报错. 多刺激!
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2019/08/09
 * Copyright (c) 2019 ShenZhen O&M Cloud Co., Ltd. All rights reserved.
 */
public class DslViewHolder extends RecyclerView.ViewHolder {

    /**
     * findViewById是循环枚举所有子View的, 多少也是消耗性能的, +一个缓存
     */
    private SparseArray<WeakReference<View>> sparseArray;

    public DslViewHolder(@NonNull View itemView) {
        super(itemView);
        sparseArray = new SparseArray<>(32);
    }

    public <T extends View> T v(@IdRes int resId) {
        WeakReference<View> viewWeakReference = sparseArray.get(resId);
        View view;
        if (viewWeakReference == null) {
            view = itemView.findViewById(resId);
            sparseArray.put(resId, new WeakReference<>(view));
        } else {
            view = viewWeakReference.get();
            if (view == null) {
                view = itemView.findViewById(resId);
                sparseArray.put(resId, new WeakReference<>(view));
            }
        }
        return (T) view;
    }

    /**
     * 单击某个View
     */
    public void clickView(View view) {
        if (view != null) {
            view.performClick();
        }
    }

    /**
     * 清理缓存
     */
    public void clear() {
        sparseArray.clear();
    }

    public void click(@IdRes int id, final View.OnClickListener listener) {
        View view = v(id);
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    public void clickItem(final View.OnClickListener listener) {
        click(itemView, listener);
    }

    public void click(View view, final View.OnClickListener listener) {
        if (view != null) {
            view.setOnClickListener(listener);
        }
    }

    public void post(Runnable runnable) {
        itemView.post(runnable);
    }

    public void postDelay(Runnable runnable, long delayMillis) {
        itemView.postDelayed(runnable, delayMillis);
    }

    public void postDelay(long delayMillis, Runnable runnable) {
        postDelay(runnable, delayMillis);
    }

    public void removeCallbacks(Runnable runnable) {
        itemView.removeCallbacks(runnable);
    }

    public TextView tv(@IdRes int resId) {
        return (TextView) v(resId);
    }

    public ImageView img(@IdRes int resId) {
        return (ImageView) v(resId);
    }

    public RecyclerView rv(@IdRes int resId) {
        return (RecyclerView) v(resId);
    }

    public ViewGroup group(@IdRes int resId) {
        return group(v(resId));
    }

    public ViewGroup group(View view) {
        return (ViewGroup) view;
    }

    public <T extends View> T focus(@IdRes int resId) {
        View v = v(resId);
        if (v != null) {
            v.setFocusable(true);
            v.setFocusableInTouchMode(true);
            v.requestFocus();
            return (T) v;
        }
        return null;
    }

    public View view(@IdRes int resId) {
        return v(resId);
    }

    public boolean isVisible(@IdRes int resId) {
        return v(resId).getVisibility() == View.VISIBLE;
    }

    public View visible(@IdRes int resId) {
        return visible(v(resId));
    }

    public DslViewHolder visible(@IdRes int resId, boolean visible) {
        View view = v(resId);
        if (visible) {
            visible(view);
        } else {
            gone(view);
        }
        return this;
    }

    public DslViewHolder invisible(@IdRes int resId, boolean visible) {
        View view = v(resId);
        if (visible) {
            visible(view);
        } else {
            invisible(view);
        }
        return this;
    }

    public View visible(View view) {
        if (view != null) {
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
            }
        }
        return view;
    }

    public DslViewHolder enable(@IdRes int resId, boolean enable) {
        View view = v(resId);
        enable(view, enable);
        return this;
    }

    private void enable(View view, boolean enable) {
        if (view == null) {
            return;
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                enable(((ViewGroup) view).getChildAt(i), enable);
            }
        } else {
            if (view.isEnabled() != enable) {
                view.setEnabled(enable);
            }
            if (view instanceof EditText) {
                view.clearFocus();
            }
        }
    }

    public View invisible(@IdRes int resId) {
        return invisible(v(resId));
    }

    public View invisible(View view) {
        if (view != null) {
            if (view.getVisibility() != View.INVISIBLE) {
                view.clearAnimation();
                view.setVisibility(View.INVISIBLE);
            }
        }
        return view;
    }

    public void gone(@IdRes int resId) {
        gone(v(resId));
    }

    public DslViewHolder gone(View view) {
        if (view != null) {
            if (view.getVisibility() != View.GONE) {
                view.clearAnimation();
                view.setVisibility(View.GONE);
            }
        }
        return this;
    }

}
