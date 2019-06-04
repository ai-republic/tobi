/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airepublic.microprofile.core.pathmatcher;

import java.util.Map;

public class MappingResult<T> {

    private final T mappedObject;
    private final Map<String, String> pathParams;


    public MappingResult(final T mappedObject, final Map<String, String> pathParams) {
        this.mappedObject = mappedObject;
        this.pathParams = pathParams;
    }


    public T getMappedObject() {
        return mappedObject;
    }


    public Map<String, String> getPathParams() {
        return pathParams;
    }
}
