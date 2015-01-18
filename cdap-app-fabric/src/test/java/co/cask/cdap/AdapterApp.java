/*
 * Copyright © 2015 Cask Data, Inc.
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

package co.cask.cdap;

import co.cask.cdap.api.app.AbstractApplication;
import co.cask.cdap.api.data.stream.Stream;
import co.cask.cdap.api.mapreduce.AbstractMapReduce;
import co.cask.cdap.api.workflow.Workflow;
import co.cask.cdap.api.workflow.WorkflowSpecification;

/**
 *  App to test adapter lifecycle.
 */
public class AdapterApp extends AbstractApplication {

  @Override
  public void configure() {
    setName("AdapterApp");
    addStream(new Stream("mySource"));
    setDescription("Application for to test Adapter lifecycle");
    addWorkflow(new AdapterWorkflow());
  }

  //TODO: Move to configurer API
  public static class AdapterWorkflow implements Workflow {

    @Override
    public WorkflowSpecification configure() {
      return WorkflowSpecification.Builder.with()
        .setName("AdapterWorkflow")
        .setDescription("Workflow to test Adaoter")
        .onlyWith(new DummyMapReduceJob())
        .build();
    }
  }

  public static class DummyMapReduceJob extends AbstractMapReduce {

    @Override
    protected void configure() {
      setDescription("Mapreduce that does nothing (and actually doesn't run) - it is here to test Adapter lifecycle");
    }
  }
}
