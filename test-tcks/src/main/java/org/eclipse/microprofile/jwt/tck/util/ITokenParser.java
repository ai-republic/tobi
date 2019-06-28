/*
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
 *
 *  See the NOTICE file(s) distributed with this work for additional
 *  information regarding copyright ownership.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.eclipse.microprofile.jwt.tck.util;

import java.security.PublicKey;

import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * The TCK requires test harness integration code to bridge between the unit tests and vendor implementations.
 * An implementation ITokenParser interface
 * @deprecated This will be removed in the 2.0 release as it is no longer used by the TCK.
 */
@Deprecated()
public interface ITokenParser {
    /**
     *
     * @param bearerToken the bearer token to parse and validate
     * @param issuer issuer to validate the bearer token issuer against
     * @param signedBy the public key used to validate the bearer token signature
     * @return the parsed JWTPrincipal representation
     * @throws Exception thrown on parse or validation failure
     */
    JsonWebToken parse(String bearerToken, String issuer, PublicKey signedBy) throws Exception;
}
