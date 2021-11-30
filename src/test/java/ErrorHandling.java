import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;


public class ErrorHandling {
    public static void log(Object object) {
        System.out.print(object);
        System.out.println();
    }

    @Test
    void programmaticPublisher() {
        Flux<String> strPublisher = Flux.generate(
                AtomicInteger::new,
                (mutableInt, publishIt) -> {
                    publishIt.next(String.format(" on next value:: %s", mutableInt.getAndIncrement()));
                    if (mutableInt.get() == 17) {
                        publishIt.complete();
                    }
                    return mutableInt;
                });
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_errorThrown() {
        Flux<String> strPublisher = Flux.generate(
                AtomicInteger::new,
                (mutableInt, publishIt) -> {
                    publishIt.next(String.format(" on next value:: %s", mutableInt.getAndIncrement()));
                    if (mutableInt.get() == 17) {
                        throw new IllegalArgumentException("my illegal argument");
                    }
                    return mutableInt;
                });
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_errorCalledOnPublishIt() {
        Flux<String> strPublisher = Flux.generate(
                AtomicInteger::new,
                (mutableInt, publishIt) -> {
                    publishIt.next(String.format(" on next value:: %s", mutableInt.getAndIncrement()));
                    if (mutableInt.get() == 17) {
                        publishIt.error(new IllegalArgumentException("my illegal argument"));
                    }
                    return mutableInt;
                });
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_onErrorReturn_matchingMessage() {
        //try with string type inferred type
        Flux<Object> strPublisher = Flux.generate(
                        AtomicInteger::new,
                        (mutableInt, publishIt) -> {
                            publishIt.next(String.format(" on next value:: %s", mutableInt.getAndIncrement()));
                            if (mutableInt.get() == 17) {
                                throw new IllegalArgumentException("my illegal argument");
                            }
                            return mutableInt;
                        })
                .onErrorReturn(e -> e.getMessage().contains("illegal"), "onErrorReturn illegal")
                .onErrorReturn("OnErrorReturn error happened");
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_onErrorReturn_nonMatchingMessage() {
        Flux<String> strPublisher = Flux.just("a", "b", "c", "d", "e")
                .map(s -> {
                    if (s.equals("c")) throw new IllegalArgumentException("Illegal value");
                    return s;
                })
                .onErrorReturn(e -> e.getMessage().contains("nonMatchin"), "onErrorReturn illegal")
                .onErrorReturn("OnErrorReturn error happened - fallback");
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_onErrorReturn_ClassMatch() {
        Flux<String> strPublisher = Flux.just("a", "b", "c", "d", "e")
                .map(s -> {
                    if (s.equals("c")) throw new IllegalArgumentException("Illegal value");
                    return s;
                })
                .onErrorReturn(e -> e.getMessage().contains("nonMatchin"), "onErrorReturn illegal")
                .onErrorReturn(IllegalArgumentException.class, "class catched");
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_onErrorContinue() {
        Flux<String> strPublisher = Flux.just("a", "b", "c", "d", "e")
                .map(s -> {
                    if (s.equals("c")) throw new IllegalArgumentException("Illegal value");
                    return s;
                })
                .onErrorContinue((throwable, e) -> log("onErrorContinue exception is detected"));
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }

    @Test
    void programmaticPublisher_onErrorContinue_ExceptionMatched() {
        Flux<String> strPublisher = Flux.just("a", "b", "c", "d", "e")
                .map(s -> {
                    if (s.equals("c")) throw new IllegalArgumentException("Illegal value");
                    return s;
                })
                .onErrorContinue(IllegalArgumentException.class, (throwable, e) -> log("onErrorContinue IllegalArgumentException is detected"))
                .onErrorContinue((throwable, e) -> log("onErrorContinue exception is detected - fallback"));
        strPublisher.subscribe(
                s -> System.out.printf("Subscriber received:: %s%n", s),
                e -> System.out.println("Error published:: " + e),
                () -> System.out.println("Complete notification sent!"));
    }
}
