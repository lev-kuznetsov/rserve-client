/*
 * The MIT License (MIT)
 * Copyright (c) 2017 Dana-Farber Cancer Institute
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
package us.levk.rserve.client.mocks;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class StreamingMatchingRemoteMock extends RemoteMockAdapter {

  ByteArrayOutputStream m = new ByteArrayOutputStream ();
  final LinkedHashMap <byte[], Callable <?>> h = new LinkedHashMap <> ();
  Iterator <Entry <byte[], Callable <?>>> i;

  public StreamingMatchingRemoteMock add (byte[] p, Callable <?> c) {
    h.put (p, c);
    i = h.entrySet ().iterator ();
    return this;
  }

  public void sendBinary (ByteBuffer b, boolean l) throws IOException {
    byte[] o = new byte[b.limit () - b.position ()];
    b.get (o);
    m.write (o);
    if (l) {
      Entry <byte[], Callable <?>> e = i.next ();
      assertArrayEquals (e.getKey (), m.toByteArray ());
      m = new ByteArrayOutputStream ();
      try {
        e.getValue ().call ();
      } catch (Exception x) {
        throw new RuntimeException (x);
      }
    }
  }
}
