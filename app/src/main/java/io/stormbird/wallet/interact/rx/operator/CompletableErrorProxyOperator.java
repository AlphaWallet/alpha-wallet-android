package io.stormbird.wallet.interact.rx.operator;

import io.reactivex.CompletableObserver;
import io.reactivex.CompletableOperator;
import io.reactivex.observers.DisposableCompletableObserver;

public class CompletableErrorProxyOperator implements CompletableOperator {

    private final Throwable throwable;

    CompletableErrorProxyOperator(Throwable throwable) {
        this.throwable = throwable;
    }

    @Override
    public CompletableObserver apply(CompletableObserver observer) {
        return new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                if (!isDisposed()) {
                    observer.onError(throwable);
                }
            }

            @Override
            public void onError(Throwable ex) {
                if (!isDisposed()) {
                    observer.onError(ex);
                }
            }
        };
    }
}
