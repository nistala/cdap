/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package co.cask.cdap.common.http;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Return type for http requests executed by {@link HttpResponse}
 */
public class HttpResponse {
  private final int responseCode;
  private final String responseMessage;
  private final InputStream content;

  HttpResponse(int responseCode, String responseMessage, InputStream content) {
    this.responseCode = responseCode;
    this.responseMessage = responseMessage;
    this.content = content;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getResponseMessage() {
    return responseMessage;
  }

  public InputStream getContent() {
    return content;
  }

  public byte[] getResponseBody() throws IOException {
    return ByteStreams.toByteArray(content);
  }

  public String getResponseBodyAsString() throws IOException {
    return new String(getResponseBody(), Charsets.UTF_8);
  }

  public String getResponseBodyAsString(Charset charset) throws IOException {
    return new String(getResponseBody(), charset);
  }
}
