/*
 * Copyright 2014 Cask, Inc.
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

package co.cask.cdap.examples.purchase;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.DataSetContext;
import co.cask.cdap.api.dataset.Dataset;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.api.service.AbstractService;
import co.cask.cdap.api.service.AbstractServiceWorker;
import co.cask.cdap.api.service.TxRunnable;
import co.cask.cdap.api.service.http.AbstractHttpServiceHandler;
import co.cask.cdap.api.service.http.HttpServiceRequest;
import co.cask.cdap.api.service.http.HttpServiceResponder;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * A Catalog Lookup Service implementation that provides ids for products.
 */
public class CatalogLookupService extends AbstractService {

  @Override
  protected void configure() {
    setName(PurchaseApp.SERVICE_NAME);
    setDescription("Service to lookup product ids.");
    addHandler(new ProductCatalogLookup());
    addWorker(new ExampleWorker());
  }

  /**
   * Lookup Handler to serve requests.
   */
  @Path("/v1")
  public static final class ProductCatalogLookup extends AbstractHttpServiceHandler {

    @Path("product/{id}/catalog")
    @GET
    public void handler(HttpServiceRequest request, HttpServiceResponder responder, @PathParam("id") String id) {
      // send string Cat-<id> with 200 OK response.
      responder.sendString(200, "Cat-" + id, Charsets.UTF_8);
    }
  }

  /**
   * Example Worker.
   */
  public static final class ExampleWorker extends AbstractServiceWorker {

    @Override
    public void stop() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void run() {

      try {
        for (int i = 0; i < 100; i++) {
          final int finalI = i++;

          getContext().execute(new TxRunnable() {
            @Override
            public void run(DataSetContext context) {
              KeyValueTable dataset = context.getDataSet("testTable");
              dataset.write("testKey", Bytes.toBytes(finalI));
            }
          });

          Thread.sleep(5000);
        }
      } catch (Exception e) {
        Throwables.propagate(e);
      }
    }
  }
}
