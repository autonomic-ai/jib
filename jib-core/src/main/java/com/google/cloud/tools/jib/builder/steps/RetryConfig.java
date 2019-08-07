package com.google.cloud.tools.jib.builder.steps;

class RetryConfig {

  private final int maxRetries;
  private final int backoffDelay;
  private final int maxDelay;
  private final double delayFactor;

  RetryConfig(int maxRetries, int backoffDelay, int maxDelay, double delayFactor) {
    this.maxRetries = maxRetries;
    this.backoffDelay = backoffDelay;
    this.maxDelay = maxDelay;
    this.delayFactor = delayFactor;
  }

  int getMaxRetries() {
    return maxRetries;
  }

  int getBackoffDelay() {
    return backoffDelay;
  }

  int getMaxDelay() {
    return maxDelay;
  }

  double getDelayFactor() {
    return delayFactor;
  }
}
