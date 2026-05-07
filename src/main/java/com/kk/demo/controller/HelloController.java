package com.kk.demo.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <br>
 *
 * @author mm
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HelloController implements SmartInitializingSingleton {
  private final MeterRegistry meterRegistry;

  private final OpenTelemetry openTelemetry;

  @GetMapping("/hello")
  public String hello() {
    log.info("enter hello.");
    meterRegistry.counter("hello.count").increment();
    return meterRegistry.timer("hello.cost").record((this::doSomething));
  }

  private String doSomething() {
    return "Hello World!";
  }

  @Override
  public void afterSingletonsInstantiated() {
    log.info("openTelemetry:{}", openTelemetry);
  }
}
