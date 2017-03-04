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
package us.levk.rserve.client.protocol.commands;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static us.levk.rserve.client.protocol.Qap.CMD_readFile;
import static us.levk.rserve.client.protocol.Qap.integer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Command to read a buffer from the opened file
 * 
 * @author levk
 */
public class Read implements Command <ByteBuffer> {

  private final int request;

  public Read (int r) {
    request = r;
  }

  /*
   * (non-Javadoc)
   * 
   * @see us.levk.rserve.client.protocol.commands.Command#encode(com.fasterxml.
   * jackson.databind.ObjectMapper)
   */
  @Override
  public Stream <ByteBuffer> encode (ObjectMapper m) throws IOException {
    List <ByteBuffer> n = integer (request).collect (toList ());
    return concat (of (allocate (16).order (LITTLE_ENDIAN).putInt (0, CMD_readFile).putInt (4, n.stream ().map (b -> {
      return b.limit () - b.position ();
    }).reduce (0, (x, y) -> x + y))), n.stream ());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * us.levk.rserve.client.protocol.commands.Command#decode(java.nio.ByteBuffer,
   * com.fasterxml.jackson.databind.ObjectMapper)
   */
  @Override
  public ByteBuffer decode (ByteBuffer c, ObjectMapper m) throws IOException {
    return c;
  }
}
