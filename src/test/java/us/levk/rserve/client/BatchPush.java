/*
 * The MIT License (MIT)
 * Copyright (c) 2017 lev.v.kuznetsov@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package us.levk.rserve.client;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

@R ("cf <- colnames(read.table('f.tsv')); rm <- rownames(read.table('m.tsv'))")
public class BatchPush {

  final File d;

  {
    try {
      d = Files.createTempDirectory ("").toFile ();
      f = c (d, "f.tsv");
    } catch (IOException e) {
      throw new RuntimeException (e);
    }
  }

  @Push File f;

  private File c (File d, String n) {
    File f = new File (d, n);
    try (PrintStream p = new PrintStream (new FileOutputStream (f))) {
      p.println ("\tc1\tc2\tc3\tc4");
      p.println ("r1\t.1\t.2\t.3\t.4");
      p.println ("r2\t.2\t.2\t.3\t.4");
      p.println ("r3\t.3\t.2\t.3\t.4");
      p.println ("r4\t.4\t.2\t.3\t.4");
      p.println ("r5\t.5\t.2\t.3\t.4");
    } catch (IOException e) {
      throw new RuntimeException (e);
    }
    return f;
  }

  @Push
  File m () {
    return c (d, "m.tsv");
  }

  @Resolve
  void cf (String[] n) {
    assertArrayEquals ("c1,c2,c3,c4".split (","), n);
  }

  @Resolve
  void rm (String[] n) {
    assertArrayEquals ("r1,r2,r3,r4,r5".split (","), n);
  }
}
