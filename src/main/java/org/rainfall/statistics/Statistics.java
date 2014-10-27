/*
 * Copyright 2014 Aurélien Broszniowski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rainfall.statistics;

import jsr166e.ConcurrentHashMapV8;

/**
 * A {@link org.rainfall.statistics.Statistics} instance holds the statistics of all results at a given point in time
 *
 * @author Aurelien Broszniowski
 */

public class Statistics {

  private final Result[] keys;
  private final ConcurrentHashMapV8<Result, Long> counters = new ConcurrentHashMapV8<Result, Long>();
  private final ConcurrentHashMapV8<Result, Double> latencies = new ConcurrentHashMapV8<Result, Double>();
  private final Long startTime;

  public Statistics(Result[] keys) {
    this.keys = keys;
    for (Result key : keys) {
      this.counters.put(key, 0L);
      this.latencies.put(key, 0.0d);
    }
    this.startTime = getTime();
  }

  public void increaseCounterAndSetLatency(Result result, Long latency) {
    //TODO improve the atomicity
    synchronized (counters.get(result)) {
      long cnt = this.counters.get(result);
      double updatedLatency = (this.latencies.get(result) * cnt + (latency / 1000000L)) / (cnt + 1);
      this.latencies.put(result, updatedLatency);  //TODO : use merge
      this.counters.put(result, ++cnt);          //TODO use merge
    }
  }

  public Result[] getKeys() {
    return keys;
  }

  public Long getCounter(Result key) {
    return counters.get(key);
  }

  public Double getLatency(Result key) {
    return latencies.get(key);
  }

  public Long getTps(Result key) {
    long cnt, length;
    synchronized (startTime) {
      length = getTime() - this.startTime;
      if (length < 1000000000L) {
        return 0L;
      }
      cnt = this.counters.get(key);
    }
    return cnt / (length / 1000000000L);
  }

  protected long getTime() {
    return System.nanoTime();
  }

  public Long sumOfCounters() {
    Long total = 0L;
    synchronized (counters) {
      for (Result key : keys) {
        total += counters.get(key);
      }
    }
    return total;
  }

  public Double averageLatencyInMs() {
    Double average = 0.0d;
    synchronized (latencies) {
      int counter = 0;
      for (Result key : keys) {
        double latency = latencies.get(key);
        if (latency > 0) {
          average += latency;
          counter++;
        }
      }
      average /= counter;
    }
    return average;
  }

  public Long averageTps() {
    long cnt, length;
    synchronized (startTime) {
      length = getTime() - this.startTime;
      if (length < 1000000000L) {
        return 0L;
      }
      cnt = sumOfCounters();
    }
    return cnt / (length / 1000000000L);
  }
}