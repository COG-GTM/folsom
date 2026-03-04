/*
 * Copyright (c) 2014-2015 Spotify AB
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

package com.spotify.folsom.client;

import com.google.common.base.Preconditions;
import com.spotify.folsom.AbstractRawMemcacheClient;
import com.spotify.folsom.ConnectionChangeListener;
import com.spotify.folsom.ObservableClient;
import com.spotify.folsom.RawMemcacheClient;
import com.spotify.folsom.ketama.AddressAndClient;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class AbstractMultiMemcacheClient extends AbstractRawMemcacheClient
    implements ConnectionChangeListener {

  protected final Collection<RawMemcacheClient> clients;

  public AbstractMultiMemcacheClient(final Collection<RawMemcacheClient> clients) {
    Preconditions.checkArgument(!clients.isEmpty(), "clients must not be empty");
    this.clients = clients;
    clients.forEach(client -> client.registerForConnectionChanges(this));
  }

  @Override
  public void shutdown() {
    clients.forEach(RawMemcacheClient::shutdown);
  }

  @Override
  public boolean isConnected() {
    return clients.stream().anyMatch(RawMemcacheClient::isConnected);
  }

  @Override
  public int numTotalConnections() {
    return clients.stream().mapToInt(RawMemcacheClient::numTotalConnections).sum();
  }

  @Override
  public int numActiveConnections() {
    return clients.stream().mapToInt(RawMemcacheClient::numActiveConnections).sum();
  }

  @Override
  public int numPendingRequests() {
    return this.clients.stream().mapToInt(RawMemcacheClient::numPendingRequests).sum();
  }

  @Override
  public Stream<AddressAndClient> streamNodes() {
    return clients.stream().flatMap(RawMemcacheClient::streamNodes);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + clients + ")";
  }

  @Override
  public void connectionChanged(ObservableClient client) {
    notifyConnectionChange();
  }

  @Override
  public Throwable getConnectionFailure() {
    return clients.stream()
        .map(RawMemcacheClient::getConnectionFailure)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }
}
