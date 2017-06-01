package io.vertx.ext.metric.collect.impl;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class GroupTest {

//  @Test
//  @Ignore
//  public void shouldGroupMetrics() {
//    List<DataPoint> input = new ArrayList<>();
//    input.add(new CounterPoint("counter", 1L, 2L));
//    input.add(new GaugePoint("gauge", 2L, 2D));
//    input.add(new AvailabilityPoint("availability", 3L, "2"));
//    
//    Map<? extends Class<? extends DataPoint>, Map<String, List<DataPoint>>> mixedData;
//    mixedData = input.stream().collect(groupingBy(DataPoint::getClass, groupingBy(DataPoint::getName)));
//
//    System.out.println(mixedData.size());
//    System.out.println(mixedData.keySet());
//    System.out.println(mixedData.values());
//
//  }
}
