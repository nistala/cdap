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

package co.cask.cdap.examples.purchase;

import co.cask.cdap.api.service.AbstractService;
import co.cask.cdap.api.service.AbstractServiceWorker;
import co.cask.cdap.api.service.ServiceWorkerContext;
import co.cask.cdap.api.service.http.AbstractHttpServiceHandler;
import co.cask.cdap.api.service.http.HttpServiceRequest;
import co.cask.cdap.api.service.http.HttpServiceResponder;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.Service;
import org.apache.twill.common.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * A Catalog Lookup Service implementation that provides ids for products.
 */
public class CatalogLookupService extends AbstractService {
  private static final Logger LOG = LoggerFactory.getLogger(CatalogLookupService.class);

  @Override
  protected void configure() {
    setName(PurchaseApp.SERVICE_NAME);
    setDescription("Service to lookup product ids.");
    addHandler(new ProductCatalogLookup());
    addWorker(new GuavaServiceWorker(new CrawlingService()));
  }

  /**
   * A Catalog Lookup Service implementation that provides ids for products.
   */
  public static class CrawlingService extends AbstractScheduledService {
    private static final Logger LOG = LoggerFactory.getLogger(CrawlingService.class);

    protected void startUp() throws Exception {
      LOG.info("started up");
    }

    protected void runOneIteration() throws Exception {
      LOG.info("run one iteration");
    }

    protected void shutDown() throws Exception {
      LOG.info("started up");
    }

    @Override
    protected Scheduler scheduler() {
      return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.SECONDS);
    }
  }

  public class GuavaServiceWorker extends AbstractServiceWorker {

    private com.google.common.util.concurrent.Service service;
    private Future<Service.State> completion;

    public GuavaServiceWorker(com.google.common.util.concurrent.Service service) {
      this.service = service;
    }

    @Override
    protected void configure() {
      setProperties(ImmutableMap.of("service.class", service.getClass().getName()));
    }

    @Override
    public void initialize(ServiceWorkerContext context) throws Exception {
      super.initialize(context);
      String serviceClassName = context.getSpecification().getProperties().get("service.class");
      service = (com.google.common.util.concurrent.Service) Class.forName(serviceClassName).newInstance();
      completion = Services.getCompletionFuture(service);
      service.start();
    }

    @Override
    public void run() {
      try {
        completion.get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        LOG.error("Caught exception while running guava service: ", e);
      }
    }

    @Override
    public void stop() {
      service.stop();
    }
  }


  /**
   * Lookup Handler to serve requests.
   */
  @Path("/v1")
  public static final class ProductCatalogLookup extends AbstractHttpServiceHandler {

    @Path("product/{id}/catalog")
    @GET
    public void handler(HttpServiceRequest request, HttpServiceResponder responder, @PathParam("id") String id) {
      // send string Catalog-<id> with 200 OK response.
      responder.sendString(200, "Catalog-" + id, Charsets.UTF_8);
    }
  }
}
