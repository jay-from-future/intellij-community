// "Create missing branches 'B' and 'E'" "true"
package com.siyeh.ipp.enumswitch;

class BeforeDefault {
  enum X {A, B, C, D, E, F}
  
  String test(X x) {
    return switch (x)<caret> {
      case A -> "foo";
      case C, D -> "bar";
      case F -> "baz";
      default -> throw new AssertionError();
    };
  }
}