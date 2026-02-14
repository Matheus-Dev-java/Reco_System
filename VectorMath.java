package com.escoladoestudante.reco.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class VectorMath {
  private VectorMath() {}

  public static double dot(List<Double> a, List<Double> b) {
    int n = Math.min(a.size(), b.size());
    double s = 0.0;
    for (int i = 0; i < n; i++) s += a.get(i) * b.get(i);
    return s;
  }

  public static double norm(List<Double> a) {
    double s = 0.0;
    for (double x : a) s += x * x;
    return Math.sqrt(s);
  }

  public static double cosine(List<Double> a, List<Double> b) {
    double d = dot(a, b);
    double na = norm(a);
    double nb = norm(b);
    if (na == 0.0 || nb == 0.0) return 0.0;
    return d / (na * nb);
  }

  public static List<Double> addScaled(List<Double> acc, List<Double> v, double w) {
    int n = Math.min(acc.size(), v.size());
    for (int i = 0; i < n; i++) acc.set(i, acc.get(i) + v.get(i) * w);
    return acc;
  }

  public static List<Double> zeros(int n) {
    return IntStream.range(0, n).mapToObj(i -> 0.0).collect(Collectors.toList());
  }

  public static List<Double> scale(List<Double> v, double w) {
    for (int i = 0; i < v.size(); i++) v.set(i, v.get(i) * w);
    return v;
  }
}
