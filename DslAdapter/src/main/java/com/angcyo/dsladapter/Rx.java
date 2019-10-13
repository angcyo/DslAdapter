package com.angcyo.dsladapter;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;

/**
 * Email:angcyo@126.com
 *
 * @author angcyo
 * @date 2019/10/02
 */
public class Rx {
    public static <T> Observable<T> create(final Func<T> doFunc) {
        return Observable.create(new SyncOnSubscribe<Integer, T>() {
            @Override
            protected Integer generateState() {
                return 1;
            }

            @Override
            protected Integer next(Integer state, Observer<? super T> observer) {
                //L.e("next-----() -> " + state);
                if (state <= 0) {
                    observer.onCompleted();
                } else {
                    observer.onNext(doFunc.call(observer));
                }
                return 0;
            }
        }).compose(Rx.<T>defaultTransformer());
    }

    /**
     * 默认调度转换
     */
    public static <T> Observable.Transformer<T, T> defaultTransformer() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> tObservable) {
                return tObservable.unsubscribeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

    public interface Func<T> extends Func1<Observer, T> {
    }
}
