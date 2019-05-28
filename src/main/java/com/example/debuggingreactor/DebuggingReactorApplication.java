package com.example.debuggingreactor;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Log4j2
@SpringBootApplication
public class DebuggingReactorApplication {


	public static void main(String[] args) {
//		Hooks.onOperatorDebug();

//		ReactorDebugAgent.init();
//		ReactorDebugAgent.processExistingClasses();

		BlockHound.install();

		SpringApplication.run(DebuggingReactorApplication.class, args);
	}

	static class IllegalLetterException extends RuntimeException {
		public IllegalLetterException() {
			super("can't be an F! No F's!");
		}
	}

	void info(String letter) {
		log.error("THREAD:" + Thread.currentThread().getName());
		log.info("the current value is  " + letter);
	}

	void error(Throwable t) {
		log.error("THREAD:" + Thread.currentThread().getName());
		log.error("OH NOES!");
		log.error(t);
	}


	void processPublisher(Flux<String> letters) {
		letters.subscribe(this::info);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void go() throws Exception {

		Scheduler cantBlock = Schedulers.newParallel("p5", 5);
		Scheduler canBlock = Schedulers.elastic();

		Flux<String> letters = Flux
			.just("A", "B", "C", "D", "E", "F")
			.checkpoint("capital letters", false)
			.map(String::toLowerCase)
			.checkpoint("lowercase letters", false)
			.flatMap(Mono::just)
			.doOnNext(x -> block())
			.checkpoint("i want an error to show this message", false)
			.subscribeOn(canBlock, true)
			.log()
			.delayElements(Duration.ofSeconds(1))
			.doOnError(this::error);

		processPublisher(letters);
//

	}

	@SneakyThrows
	void block() {
		Thread.sleep(1000);
	}

}
