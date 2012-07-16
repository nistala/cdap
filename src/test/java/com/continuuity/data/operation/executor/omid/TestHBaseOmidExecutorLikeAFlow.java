package com.continuuity.data.operation.executor.omid;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.continuuity.data.hbase.HBaseTestBase;
import com.continuuity.data.operation.executor.OperationExecutor;
import com.continuuity.data.runtime.DataFabricDistributedModule;
import com.continuuity.data.table.OVCTableHandle;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestHBaseOmidExecutorLikeAFlow extends TestOmidExecutorLikeAFlow {

  private static Injector injector;

  private static OmidTransactionalOperationExecutor executor;

  private static OVCTableHandle handle;

  @BeforeClass
  public static void startEmbeddedHBase() {
    try {
      HBaseTestBase.startHBase();
      injector = Guice.createInjector(
          new DataFabricDistributedModule(HBaseTestBase.getConfiguration()));
      executor = (OmidTransactionalOperationExecutor)injector.getInstance(
          OperationExecutor.class);
      handle = executor.getTableHandle();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterClass
  public static void stopEmbeddedHBase() {
    try {
      HBaseTestBase.stopHBase();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected OmidTransactionalOperationExecutor getOmidExecutor() {
    return executor;
  }

  @Override
  protected OVCTableHandle getTableHandle() {
    return handle;
  }

  @Override
  protected int getNumIterations() {
    return 100;
  }

  // Test Overrides

  /**
   * Clear currently not enabled for HBase.  Support implemented in ENG-422.
   */
  @Test @Override @Ignore
  public void testClearFabric() throws Exception {}

  /**
   * Currently not working.  Will be fixed in ENG-421.
   */
  @Test @Override @Ignore
  public void testUserReadOwnWritesAndWritesStableSorted() throws Exception {}
}
