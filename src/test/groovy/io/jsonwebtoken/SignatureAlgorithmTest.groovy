/*
 * Copyright (C) 2014 jsonwebtoken.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jsonwebtoken

import org.testng.annotations.Test
import static org.testng.Assert.*

class SignatureAlgorithmTest {

    @Test
    void testNames() {
        def algNames = ['HS256', 'HS384', 'HS512', 'RS256', 'RS384', 'RS512',
                        'ES256', 'ES384', 'ES512', 'PS256', 'PS384', 'PS512', 'NONE']

        for( String name : algNames ) {
            testName(name)
        }
    }

    private static void testName(String name) {
        def alg = SignatureAlgorithm.forName(name);
        def namedAlg = name as SignatureAlgorithm //Groovy type coercion FTW!
        assertTrue alg == namedAlg
        assert alg.description != null && alg.description != ""
    }

    @Test(expectedExceptions = SignatureException)
    void testUnrecognizedAlgorithmName() {
        SignatureAlgorithm.forName('whatever')
    }
}
