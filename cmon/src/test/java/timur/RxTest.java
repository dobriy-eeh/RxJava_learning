package timur;

import org.junit.Test;
import rx.*;
import rx.functions.Action2;
import rx.observables.JoinObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.util.async.Async;
import rx.util.async.StoppableObservable;

import java.util.Random;
import java.util.function.Consumer;

/**
 * @author t.bukharaev.
 */
public final class RxTest {

    @Test
    public void helloWorld() {
        Observable<String> myObservable = Observable.create(
                new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> sub) {
                        sub.onNext("Hello, world!");
                        sub.onNext("Hello, world!");
                        sub.onCompleted();
                    }
                }
        );

        Subscriber<String> mySubscriber = new Subscriber<String>() {
            @Override
            public void onNext(String s) {
                System.out.println(s);
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }
        };

        myObservable.subscribe(mySubscriber);
    }

    @Test
    public void testThread() {
        Observable.range(0, 5).
                map(i -> {
                    log("map " + i);
                    return i * i;
                }).observeOn(myScheduler()).
                subscribe(i -> {
                    log("subscribe " + i);
                });

        sleep();

        myScheduler();
    }

    @Test
    public void testAsync() {
        final StoppableObservable<Object> observable = Async.runAsync(myScheduler(), new Action2<Observer<? super Integer>, Subscription>() {
            @Override
            public void call(Observer<? super Integer> observer, Subscription subscription) {
                log("call");
                observer.onNext(7);
                observer.onCompleted();
            }
        });


        observable.subscribe(i -> {
            log("subscribe = " + i);
        });

        sleep();
    }

    public interface Net {
        void sendRequestGetResponse(String request, Consumer<String> responseCallback);
    }

    @Test
    public void testNet() {

        Net net = getNet();

        PublishSubject<String> subject1 = PublishSubject.<String>create();
        PublishSubject<String> subject2 = PublishSubject.<String>create();
        PublishSubject<String> subject3 = PublishSubject.<String>create();

        Scheduler scheduler = myScheduler();

        Observable.zip(subject1.observeOn(scheduler), subject2.observeOn(scheduler), subject3.observeOn(scheduler), (r1, r2, r3) -> {
            log("zip " + r1 + ", " + r2 + ", " + r3);
            return r1 + " + " + r2 + " + " + r3;
        }).subscribe(s -> {
            log("subscribe " + s);
        });

        net.sendRequestGetResponse("ping1", response -> {
            log("get response " + response);
            subject1.onNext(response);
            subject1.onCompleted();
        });

        net.sendRequestGetResponse("ping2", response -> {
            log("get response " + response);
            subject2.onNext(response);
            subject2.onCompleted();
        });

        net.sendRequestGetResponse("ping3", response -> {
            log("get response " + response);
            subject3.onNext(response);
            subject3.onCompleted();
        });

        sleep(100);
    }

    private static Net getNet() {
        return (request, responseCallback) ->
                new Thread(() -> {
                    sleep(new Random().nextInt(50));
                    responseCallback.accept("hi, " + request + "!");
                }, "netThread").start();
    }

    @Test
    public void testZip() {

        final Observable<Integer> observable0 = Observable.create(subscriber ->
                new Thread(() -> {
                    sleep(100);
                    log("onNext");
                    subscriber.onNext(7);
                    subscriber.onNext(3);
                    subscriber.onNext(4);
                    subscriber.onCompleted();
                }, "mythread0").start());

        final Observable<String> observable1 = Observable.just("a", "b");

        final Observable<String> zip = Observable.zip(observable0, observable1, (i1, i2) -> {
            log("zip " + i1 + ", " + i2);
            return i1 + i2;
        });

        log("make subscription");
        zip.observeOn(myScheduler()).subscribe(i -> {
            log("subscribe " + i);
        });

        sleep(200);
    }

    @Test
    public void testMerge() {

        Observable.range(0, 1);


        JoinObservable.from(Observable.range(0, 1)).and(Observable.range(0, 2)).and(Observable.range(0, 1)).then((i0, i1, i2) -> {
            log("i0 = " + i0 + ", i1 = " + i1 + ", i2 = " + i2);
            return i0 + i1 + i2;
        });
        //Observable.co
    }

    @Test
    public void testAsync2() {
        final Observable<Integer> observable = Async.fromCallable(() -> {
            log("call = " + 5);
            return 5;
        });

        observable.subscribe(i -> {
            log("subscribe = " + i);
        });
    }

    private static Scheduler myScheduler() {
        return Schedulers.from(command -> new Thread(command, "myThread").start());
    }

    private static void log(String message) {
        System.out.println(message + ", thread = " + Thread.currentThread().getName());
    }

    private static void sleep() {
        sleep(100);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
