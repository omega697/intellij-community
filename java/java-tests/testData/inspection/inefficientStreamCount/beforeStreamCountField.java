// "Replace with 'Collection.size()'" "true-preview"

import java.util.Arrays;

class Test {
  long cnt = Arrays.asList('d', 'e', 'f')./*stream*/stream()./*count*/c<caret>ount()/*after*/;
}