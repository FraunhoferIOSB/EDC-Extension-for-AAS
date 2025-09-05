/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.app.pipeline.helper;

import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Filter an input collection.
 *
 * @param <T> Type of input.
 */
public class Filter<T> extends PipelineStep<Collection<T>, Collection<T>> {

    private final Predicate<T> filterFunction;

    private String name;

    public Filter(Predicate<T> filterFunction) {
        this.filterFunction = filterFunction;
    }

    public Filter(Predicate<T> filterFunction, String name) {
        this.filterFunction = filterFunction;
        this.name = name;
    }

    @Override
    public String getName() {
        return name != null ? name : super.getName();
    }

    @Override
    public PipelineResult<Collection<T>> apply(Collection<T> ts) {
        var filtered = ts.stream().filter(filterFunction).toList();
        if (filtered.size() != ts.size()) {
            var messages = new ArrayList<String>();
            messages.add(String.format("Filtered %s elements", ts.size() - filtered.size()));
            
            return PipelineResult.recoverableFailure(filtered, PipelineFailure.info(messages));
        }
        return PipelineResult.success(filtered);
    }
}
