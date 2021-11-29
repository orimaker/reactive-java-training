import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FirstDemos {

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
    void stepVerifier() {
        List<String> strings = Arrays.asList("asd", "sdf", "dfg", "fgh", "ghj", "hjk", "jl");
        Flux<String> stringFlux = Flux.fromIterable(strings);
        stringFlux.doOnComplete(() -> System.out.println("completed"));
        stringFlux.doOnNext(v -> System.out.println("next: " + v));
        StepVerifier.create(stringFlux)
                .expectNext("asd")
                .expectNext("sdf", "dfg", "fgh")
                .expectNextSequence(Arrays.asList("ghj", "hjk", "jl"))
                .verifyComplete();
    }
}
