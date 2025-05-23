/*
 * Copyright 2021 Confluent Inc.
 *
 * Licensed under the Confluent Community License (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.confluent.io/confluent-community-license
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package io.confluent.kafkarest.integration.accesslist;

import static io.confluent.kafkarest.TestUtils.TEST_WITH_PARAMETERIZED_QUORUM_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.core.Response.Status;
import java.util.Properties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ResourceAllowlistAndBlocklistTest extends ResourceAccesslistTestBase {

  @Override
  protected void overrideKafkaRestConfigs(Properties restProperties) {
    restProperties.put("api.endpoints.allowlist", "api.v3.topics.*,      api.v3.clusters.list");
    restProperties.put("api.endpoints.blocklist", "api.v3.topics.delete, api.v3.clusters.list");
  }

  @ParameterizedTest(name = TEST_WITH_PARAMETERIZED_QUORUM_NAME)
  @ValueSource(strings = {"kraft"})
  public void testAllowlistAndBlocklist(String quorum) {
    // Even though the checks are not exactly independent (i.e. topic deletion should be tried
    // after topic creation), all of them are executed in a single test, as: (1) they are touching
    // different API endpoints, for which we don't need state reset (on the contrary); (2) failures
    // can easily be correlated to a check; and (3) running only one integration test method saves
    // a significant amount of time.
    allowlistEnablesResourceClassExceptForBlocklistedMethods();
    blocklistDisablesResourceMethod();
    nonAllowlistAndNonBlocklistResourcesDisabled();
    optionsIsAlwaysAllowed();
  }

  private void allowlistEnablesResourceClassExceptForBlocklistedMethods() {
    assertEquals(Status.OK.getStatusCode(), listTopics().getStatus());
    assertEquals(Status.CREATED.getStatusCode(), createTopic().getStatus());
    assertEquals(Status.OK.getStatusCode(), getTopic().getStatus());
    assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), deleteTopic().getStatus());
  }

  private void blocklistDisablesResourceMethod() {
    assertEquals(Status.NOT_FOUND.getStatusCode(), listClusters().getStatus());
  }

  private void nonAllowlistAndNonBlocklistResourcesDisabled() {
    assertEquals(Status.NOT_FOUND.getStatusCode(), getCluster().getStatus());
    assertEquals(Status.METHOD_NOT_ALLOWED.getStatusCode(), updateClusterConfig().getStatus());
  }

  private void optionsIsAlwaysAllowed() {
    assertEquals(Status.OK.getStatusCode(), clustersOptions().getStatus());
    assertEquals(Status.OK.getStatusCode(), topicsOptions().getStatus());
  }
}
