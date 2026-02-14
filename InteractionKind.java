package com.escoladoestudante.reco.domain;

public sealed interface InteractionKind permits InteractionKind.View, InteractionKind.Like, InteractionKind.Share, InteractionKind.Bookmark {
  String code();

  record View() implements InteractionKind { public String code() { return "VIEW"; } }
  record Like() implements InteractionKind { public String code() { return "LIKE"; } }
  record Share() implements InteractionKind { public String code() { return "SHARE"; } }
  record Bookmark() implements InteractionKind { public String code() { return "BOOKMARK"; } }

  static InteractionKind fromCode(String code) {
    return switch (code) {
      case "VIEW" -> new View();
      case "LIKE" -> new Like();
      case "SHARE" -> new Share();
      case "BOOKMARK" -> new Bookmark();
      default -> throw new IllegalArgumentException("Unknown interaction kind: " + code);
    };
  }
}
