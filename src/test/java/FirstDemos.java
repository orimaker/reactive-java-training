import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class FirstDemos {
    public static void log(Object object) {
        System.out.print(object);
        System.out.println();
    }

    @Test
    void firstPublisher() {
        Mono.just(UUID.randomUUID())
                .log()
                .subscribe();
    }

    @Test
    void firstPublisher_withoutLogClutter() {
        Mono.just(UUID.randomUUID())
                .subscribe(System.out::println);
    }

    @Test
    void multiValuePublisher() {
        Flux.just(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
                .log()
                .subscribe(System.out::println);
    }

    @Test
    void notHandlingError() {
        Mono.error(new IllegalArgumentException("what just happened"))
                .subscribe(System.out::println);
    }

    @Test
    void errorHandling() {
        Mono.error(new IllegalArgumentException("what just happened"))
                .subscribe(
                        System.out::println,
                        e -> System.out.println("error happened: " + e.toString()));
    }

    @Test
    void doOnMethods() {
        Mono<UUID> publisher = Mono.just(UUID.randomUUID())
                .doOnSubscribe(sub -> System.out.println("doOnSubscribe " + sub))
                .doOnRequest(req -> System.out.println("doOnRequest " + req))
                .doOnSuccess(suc -> System.out.println("doOnSuccess " + suc));
        publisher.subscribe(
                System.out::println,
                e -> System.out.println("error happened: " + e.toString()),
                () -> System.out.println("completed in subscriber"));
    }

    @Test
    void stepVerifier_IMPORTANT_DIFFERENCE_buildingChainOrReference() {
        //TODO it does matter how you build your publisher, avoid references

        List<String> strings = Arrays.asList("asd", "sdf", "dfg", "fgh", "ghj", "hjk", "jl");

        log("No events will be raised");
        Flux<String> stringFluxBadlyBuilt = Flux.fromIterable(strings);
        stringFluxBadlyBuilt.doOnComplete(() -> System.out.println("completed"));
        stringFluxBadlyBuilt.doOnNext(v -> System.out.println("next: " + v));
        StepVerifier.create(stringFluxBadlyBuilt)
                .expectNext("asd")
                .expectNext("sdf", "dfg", "fgh")
                .expectNextSequence(Arrays.asList("ghj", "hjk", "jl"))
                .verifyComplete();

        log("Properly raised events");

        Flux<String> stringFluxProperlyBuilt = Flux.fromIterable(strings)
                .doOnComplete(() -> System.out.println("completed"))
                .doOnNext(v -> System.out.println("next: " + v));
        StepVerifier.create(stringFluxProperlyBuilt)
                .expectNext("asd")
                .expectNext("sdf", "dfg", "fgh")
                .expectNextSequence(Arrays.asList("ghj", "hjk", "jl"))
                .verifyComplete();
    }

    @Test
    void rangePublisher() {
        Flux<Integer> numberSeq = Flux.range(1, 20);
        numberSeq.map(v -> {
                    if (v == 8) throw new IllegalArgumentException("eight is just isn't right");
                    return v;
                })
                .subscribe(System.out::println, System.out::println);
    }

    @Test
    void rangePublisher_delayElements() throws InterruptedException {
        Flux<Integer> numberSeq = Flux.range(1, 20)
                .delayElements(Duration.ofSeconds(1))
                .log()
                .map(v -> {
                    if (v == 8) throw new IllegalArgumentException("eight is just isn't right");
                    return v;
                });

        numberSeq
                .subscribe(System.out::println);
        TimeUnit.SECONDS.sleep(10); //TODO This is important to

    }

    @Test
    void rangePublisher_Disposable() throws InterruptedException {
        Flux<Integer> numberSeq = Flux.range(1, 20).delayElements(Duration.ofSeconds(1));

        Disposable cancelRef = numberSeq.subscribe(v -> log("subscriber 1: " + v));
        Disposable keepRef = numberSeq.subscribe(v -> log("subscriber 2: " + v));
        TimeUnit.SECONDS.sleep(12);
        Disposable delayedSub = numberSeq.subscribe(v -> log("subscriber 3: " + v));
        Runnable runnableTask = () -> {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription 1");
            cancelRef.dispose();
        };

        Runnable runnableTask2 = () -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription 2");
            keepRef.dispose();
        };

        Runnable runnableTask3 = () -> {
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription 3");
            delayedSub.dispose();
        };
        runnableTask.run();
        runnableTask2.run();
        runnableTask3.run();

        //TODO Workit
    }

    @Test
    void parallel() {
        Flux<Integer> numberSeq =
                Flux.range(1, 20).delayElements(Duration.ofSeconds(3));
        Disposable cancelRef =
                numberSeq.subscribe(e -> System.out.printf("Value received %s%n", e),
                        error -> System.err.println("Error Published:: " + error),
                        () -> System.out.println("Complete event published"));
        Runnable runnableTask = () -> {
            try {
                TimeUnit.SECONDS.sleep(12);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Canceling subscription");
            cancelRef.dispose();
        };
        runnableTask.run();
    }

    @Test
    void testScheduler() {
        Scheduler reactScheduler = Schedulers.newParallel("pub-parallel", 4);
        final Flux<String> phrasePublish =
                Flux.range(1, 20)
                        .map(i -> 42 + i)
                        .publishOn(reactScheduler)
                        .map(m -> {
                            var v = Thread.currentThread().getName();
                            return String.format("%s value produced::%s", v, m);
                        });
        Runnable r0 = () -> phrasePublish.subscribe(
                n -> System.out.printf(Thread.currentThread().getName() + " subscriber recvd:: %s%n", n)
        );
        r0.run();
        Runnable r1 = () -> phrasePublish.subscribe(
                n -> System.out.printf(Thread.currentThread().getName() + " subscriber recvd:: %s%n", n)
        );
        r1.run();
    }
}
