/*
 * Copyright 2012-2013 Continuuity,Inc. All Rights Reserved.
 */
package com.continuuity.internal.app.runtime.distributed;

import com.continuuity.common.conf.CConfiguration;
import com.continuuity.data2.queue.QueueClientFactory;
import com.continuuity.data2.transaction.queue.QueueAdmin;
import com.continuuity.data2.transaction.queue.hbase.ShardedHBaseQueueAdmin;
import com.continuuity.data2.transaction.queue.hbase.ShardedHBaseQueueClientFactory;
import com.continuuity.internal.app.runtime.flow.FlowletProgramRunner;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.util.Modules;
import org.apache.hadoop.conf.Configuration;
import org.apache.twill.api.TwillContext;

/**
 *
 */
final class FlowletTwillRunnable extends AbstractProgramTwillRunnable<FlowletProgramRunner> {

  FlowletTwillRunnable(String name, String hConfName, String cConfName) {
    super(name, hConfName, cConfName);
  }

  @Override
  protected Class<FlowletProgramRunner> getProgramClass() {
    return FlowletProgramRunner.class;
  }

  @Override
  protected Module createModule(TwillContext context, CConfiguration cConf, Configuration hConf) {
    Module module = super.createModule(context, cConf, hConf);

    // TODO: Temp hack for testing sharded queue
    if (!cConf.getBoolean("data.sharded.queue", false)) {
      return module;
    }

    return Modules.override(module).with(new AbstractModule() {
      @Override
      protected void configure() {
        bind(QueueClientFactory.class).to(ShardedHBaseQueueClientFactory.class).in(Singleton.class);
        bind(QueueAdmin.class).to(ShardedHBaseQueueAdmin.class).in(Singleton.class);
      }
    });
  }
}
