package com.escoladoestudante.reco.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {
  private static final ObjectMapper M = new ObjectMapper();
  private JsonUtil() {}

  public static String toJson(Object o) {
    try { return M.writeValueAsString(o); }
    catch (Exception e) { throw new RuntimeException(e); }
  }

  public static <T> T fromJson(String s, Class<T> c) {
    try { return M.readValue(s, c); }
    catch (Exception e) { throw new RuntimeException(e); }
  }

  public static <T> T fromJson(String s, TypeReference<T> tr) {
    try { return M.readValue(s, tr); }
    catch (Exception e) { throw new RuntimeException(e); }
  }
}
