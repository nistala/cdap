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

package com.continuuity.shell.command.list;

import com.continuuity.reactor.client.ReactorAppClient;
import com.continuuity.reactor.metadata.ProgramType;
import com.continuuity.shell.command.CommandSet;

import javax.inject.Inject;

/**
 * Contains commands for listing stuff.
 */
public class ListCommandSet extends CommandSet {

  @Inject
  public ListCommandSet(ReactorAppClient appClient,
                        ListAppsCommand listAppsCommand,
                        ListAllProgramsCommand listAllProgramsCommand,
                        ListDatasetCommandSet listDatasetCommandSet,
                        ListStreamsCommand listStreamsCommand) {
    super(
      "list",
      listAppsCommand,
      listAllProgramsCommand,
      new ListProgramsCommand(ProgramType.FLOW, appClient),
      new ListProgramsCommand(ProgramType.MAPREDUCE, appClient),
      new ListProgramsCommand(ProgramType.PROCEDURE, appClient),
      new ListProgramsCommand(ProgramType.WORKFLOW, appClient),
      listDatasetCommandSet,
      listStreamsCommand
    );
  }
}
