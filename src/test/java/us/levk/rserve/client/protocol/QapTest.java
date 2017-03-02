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
package us.levk.rserve.client.protocol;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThat;
import static us.levk.rserve.client.protocol.Qap.DT_SEXP;
import static us.levk.rserve.client.protocol.Qap.DT_STRING;
import static us.levk.rserve.client.protocol.Qap.header;
import static us.levk.rserve.client.protocol.Qap.packet;
import static us.levk.rserve.client.protocol.Qap.sexp;
import static us.levk.rserve.client.protocol.Qap.string;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;

import us.levk.jackson.rserve.RserveMapper;
import us.levk.rserve.client.Streams;

public class QapTest implements Streams {

  @Test
  public void headerString6 () throws Exception {
    assertThat (header (DT_STRING, 6), is (loadb64 ("/headerString6.b64")));
  }

  @Test
  public void largeHeader () throws Exception {
    assertThat (header (DT_SEXP, 0xfffff1), is (loadb64 ("/largeHeader.b64")));
  }

  @Test
  public void stringFoobar () throws Exception {
    try (ByteArrayOutputStream o = new ByteArrayOutputStream ()) {
      string ("foobar").forEach (b -> o.write (b.array (), b.position (), b.limit () - b.position ()));
      assertArrayEquals (o.toByteArray (), loadb64 ("/stringFoobar.b64").array ());
    }
  }

  @Test
  public void sexpFooBar () throws Exception {
    try (ByteArrayOutputStream o = new ByteArrayOutputStream ();
         ByteArrayOutputStream a = new ByteArrayOutputStream ()) {
      new RserveMapper ().writeValue (o, asList ("foo", "bar"));
      sexp (o.toByteArray ()).forEach (b -> a.write (b.array (), b.position (), b.limit () - b.position ()));
      assertArrayEquals (a.toByteArray (), loadb64 ("/sexpFooBar.b64").array ());
    }
  }

  @Test
  public void emptyPacket () throws Exception {
    assertThat (packet (loadb64 ("/emptyPacket.b64").order (LITTLE_ENDIAN)), is (wrap (new byte[0])));
  }

  @Test (expected = IOException.class)
  public void badPacket () throws Exception {
    packet (loadb64 ("/badPacket.b64").order (LITTLE_ENDIAN));
  }

  @Test
  public void packetFooBar () throws Exception {
    assertThat (packet (loadb64 ("/fooBarPacket.b64").order (LITTLE_ENDIAN)), is (loadb64 ("/fooBarList.b64")));
  }
}
