/*
 * Copyright 2014 Continuuity, Inc.
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

package com.continuuity.shell.command.send;

import com.continuuity.reactor.client.ReactorStreamClient;
import com.continuuity.shell.command.AbstractCommand;
import com.continuuity.shell.completer.Completable;
import com.continuuity.shell.completer.reactor.StreamIdCompleter;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import jline.console.completer.Completer;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Sends an event to a stream.
 */
public class SendStreamEventCommand extends AbstractCommand implements Completable {

  private final ReactorStreamClient streamClient;
  private final StreamIdCompleter completer;

  @Inject
  public SendStreamEventCommand(StreamIdCompleter completer, ReactorStreamClient streamClient) {
    super("stream", "<stream-id> <stream-event>", "Sends an event to a stream");
    this.completer = completer;
    this.streamClient = streamClient;
  }

  @Override
  public void process(String[] args, PrintStream output) throws Exception {
    super.process(args, output);

    String streamId = args[0];
    String streamEvent = Joiner.on(" ").join(Arrays.copyOfRange(args, 1, args.length));
    streamClient.sendEvent(streamId, streamEvent);
  }

  @Override
  public List<? extends Completer> getCompleters(String prefix) {
    return Lists.newArrayList(prefixCompleter(prefix, completer));
  }
}
