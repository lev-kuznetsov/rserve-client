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

import static java.nio.ByteBuffer.wrap;
import static java.util.Base64.getDecoder;
import static org.apache.commons.io.IOUtils.copy;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.stream.Stream;

public interface Streams {

  default ByteBuffer loadb64 (String r) throws IOException {
    try (ByteArrayOutputStream b = new ByteArrayOutputStream ()) {
      copy (getClass ().getResourceAsStream (r), b);
      return wrap (getDecoder ().decode (b.toByteArray ()));
    }
  }

  default void assertOutput (Stream <ByteBuffer> s, String r) throws IOException {
    try (ByteArrayOutputStream a = new ByteArrayOutputStream ()) {
      s.forEach (b -> a.write (b.array (), b.position (), b.limit () - b.position ()));
      assertArrayEquals (loadb64 (r).array (), a.toByteArray ());
    }
  }

  default void b64 (Stream <ByteBuffer> s) {
    byte[] q = s.reduce (new ByteArrayOutputStream (), (o, b) -> {
      byte[] t = new byte[b.limit () - b.position ()];
      b.get (t);
      try {
        o.write (t);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace ();
      }
      return o;
    }, (x, y) -> {
      throw new UnsupportedOperationException ();
    }).toByteArray ();
    System.out.println (Base64.getEncoder ().encodeToString (q));
  }
}
