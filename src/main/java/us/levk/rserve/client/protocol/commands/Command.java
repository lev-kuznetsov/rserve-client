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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Rserve command
 * 
 * @author levk
 */
public interface Command <T> {

  /**
   * @param m
   *          mapper
   * @return stream of encoded buffers representing the command
   * @throws IOException
   *           on encoding failure
   */
  Stream <ByteBuffer> encode (ObjectMapper m) throws IOException;

  /**
   * @param c
   *          content
   * @param m
   *          mapper
   * @return decoded object
   * @throws IOException
   *           on decoding failure
   */
  default T decode (ByteBuffer c, ObjectMapper m) throws IOException {
    if (c == null || c.limit () <= c.position ()) return null;
    else throw new IOException ("Unexpected content for " + getClass ().getSimpleName () + " command");
  }
}
