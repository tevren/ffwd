/*
 * Copyright 2013-2017 Spotify AB. All rights reserved.
 *
 * The contents of this file are licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.spotify.ffwd.filter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.google.common.collect.ImmutableList;
import com.spotify.ffwd.model.Event;
import com.spotify.ffwd.model.Metric;
import lombok.Data;

import java.io.IOException;
import java.util.List;

@Data
public class AndFilter implements Filter {
    final List<Filter> terms;

    @Override
    public boolean matchesEvent(Event event) {
        for (final Filter f : terms) {
            if (!f.matchesEvent(event)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean matchesMetric(Metric metric) {
        for (final Filter f : terms) {
            if (!f.matchesMetric(metric)) {
                return false;
            }
        }

        return true;
    }

    public static class Deserializer implements FilterDeserializer.PartialDeserializer {
        @Override
        public Filter deserialize(JsonParser p, DeserializationContext ctx)
            throws IOException, JsonProcessingException {
            final ImmutableList.Builder<Filter> builder = ImmutableList.builder();

            while (p.nextToken() == JsonToken.START_ARRAY) {
                builder.add(p.readValueAs(Filter.class));
            }

            if (p.getCurrentToken() != JsonToken.END_ARRAY) {
                throw ctx.wrongTokenException(p, JsonToken.END_ARRAY, null);
            }

            final List<Filter> filters = builder.build();

            if (filters.isEmpty()) {
                return new TrueFilter();
            }

            if (filters.size() == 1) {
                return filters.iterator().next();
            }

            return new AndFilter(filters);
        }
    }
}
