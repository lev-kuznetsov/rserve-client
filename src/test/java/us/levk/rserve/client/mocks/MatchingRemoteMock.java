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
package us.levk.rserve.client.mocks;

import static java.nio.ByteBuffer.wrap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint.Basic;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class MatchingRemoteMock extends BaseMatcher <ByteBuffer> implements Basic {

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream ();

  @Override
  public void flushBatch () throws IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public boolean getBatchingAllowed () {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void sendPing (ByteBuffer arg0) throws IOException, IllegalArgumentException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void sendPong (ByteBuffer arg0) throws IOException, IllegalArgumentException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void setBatchingAllowed (boolean arg0) throws IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public boolean matches (Object item) {
    return wrap (buffer.toByteArray ()).equals (item);
  }

  @Override
  public void describeTo (Description description) {
    throw new UnsupportedOperationException ();
  }

  @Override
  public OutputStream getSendStream () throws IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public Writer getSendWriter () throws IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void sendBinary (ByteBuffer arg0) throws IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void sendBinary (ByteBuffer b, boolean arg1) throws IOException {
    byte[] a = new byte[b.limit () - b.position ()];
    b.get (a);
    buffer.write (a);
  }

  @Override
  public void sendObject (Object arg0) throws IOException, EncodeException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void sendText (String arg0) throws IOException {
    throw new UnsupportedOperationException ();
  }

  @Override
  public void sendText (String arg0, boolean arg1) throws IOException {
    throw new UnsupportedOperationException ();
  }
}
