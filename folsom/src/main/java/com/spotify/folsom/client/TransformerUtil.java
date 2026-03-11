/*
 * Copyright (c) 2015 Spotify AB
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

import com.spotify.folsom.GetResult;
import com.spotify.folsom.Transcoder;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TransformerUtil<T> {
  private final Function<GetResult<T>, T> getResultToValue;
  private final Function<List<GetResult<T>>, List<T>> listResultUnwrapper;
  private final Function<GetResult<byte[]>, GetResult<T>> resultDecoder;
  private final Function<List<GetResult<byte[]>>, List<GetResult<T>>> listResultDecoder;

  public TransformerUtil(Transcoder<T> transcoder) {
    this.getResultToValue = input -> input == null ? null : input.getValue();
    this.listResultUnwrapper =
        input -> input.stream().map(getResultToValue).collect(Collectors.toList());
    this.resultDecoder =
        input ->
            input == null
                ? null
                : GetResult.success(
                    transcoder.decode(input.getValue()), input.getCas(), input.getFlags());
    this.listResultDecoder =
        input -> input.stream().map(resultDecoder).collect(Collectors.toList());
  }

  public CompletionStage<T> unwrap(CompletionStage<GetResult<T>> future) {
    return future.thenApply(getResultToValue);
  }

  public CompletionStage<GetResult<T>> decode(CompletionStage<GetResult<byte[]>> future) {
    return future.thenApply(resultDecoder);
  }

  public CompletionStage<List<T>> unwrapList(CompletionStage<List<GetResult<T>>> future) {
    return future.thenApply(listResultUnwrapper);
  }

  public CompletionStage<List<GetResult<T>>> decodeList(
      CompletionStage<List<GetResult<byte[]>>> future) {
    return future.thenApply(listResultDecoder);
  }
}
