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
import static us.levk.rserve.client.protocol.Qap.CMD_voidEval;
import static us.levk.rserve.client.protocol.Qap.string;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Evaluate command
 * 
 * @author levk
 */
public class Evaluate implements Command <Void> {

  /**
   * R code
   */
  private final String code;

  /**
   * @param c
   *          code
   */
  public Evaluate (String c) {
    code = c;
  }

  /*
   * (non-Javadoc)
   * 
   * @see us.levk.rserve.client2.protocol.commands.Command#encode(java.io.
   * OutputStream, com.fasterxml.jackson.databind.ObjectMapper)
   */
  @Override
  public Stream <ByteBuffer> encode (ObjectMapper m) throws IOException {
    List <ByteBuffer> n = string (code).collect (toList ());
    return concat (of (allocate (16).order (LITTLE_ENDIAN).putInt (0, CMD_voidEval).putInt (4, n.stream ().map (b -> {
      return b.limit () - b.position ();
    }).reduce (0, (x, y) -> x + y))), n.stream ());
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString () {
    return "Evaluate{" + code + "}";
  }
}
