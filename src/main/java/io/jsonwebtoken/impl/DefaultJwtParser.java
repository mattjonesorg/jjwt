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
package io.jsonwebtoken.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.crypto.DefaultJwtSignatureValidator;
import io.jsonwebtoken.impl.crypto.JwtSignatureValidator;
import io.jsonwebtoken.lang.Assert;
import io.jsonwebtoken.lang.Objects;
import io.jsonwebtoken.lang.Strings;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.Key;
import java.util.Map;

@SuppressWarnings("unchecked")
public class DefaultJwtParser implements JwtParser {

    private ObjectMapper objectMapper = new ObjectMapper();

    private byte[] keyBytes;

    private Key key;

    @Override
    public JwtParser setSigningKey(byte[] key) {
        Assert.notEmpty(key, "signing key cannot be null or empty.");
        this.keyBytes = key;
        return this;
    }

    @Override
    public JwtParser setSigningKey(String base64EncodedKeyBytes) {
        Assert.hasText(base64EncodedKeyBytes, "signing key cannot be null or empty.");
        this.keyBytes = TextCodec.BASE64.decode(base64EncodedKeyBytes);
        return this;
    }

    @Override
    public JwtParser setSigningKey(Key key) {
        Assert.notNull(key, "signing key cannot be null.");
        this.key = key;
        return this;
    }

    @Override
    public boolean isSigned(String jwt) {

        if (jwt == null) {
            return false;
        }

        int delimiterCount = 0;

        for (int i = 0; i < jwt.length(); i++) {
            char c = jwt.charAt(i);

            if (delimiterCount == 2) {
                return !Character.isWhitespace(c) && c != SEPARATOR_CHAR;
            }

            if (c == SEPARATOR_CHAR) {
                delimiterCount++;
            }
        }

        return false;
    }

    @Override
    public Jwt parse(String jwt) throws MalformedJwtException, SignatureException {

        Assert.hasText(jwt, "JWT String argument cannot be null or empty.");

        String base64UrlEncodedHeader = null;
        String base64UrlEncodedPayload = null;
        String base64UrlEncodedDigest = null;

        int delimiterCount = 0;

        StringBuilder sb = new StringBuilder(128);

        for (char c : jwt.toCharArray()) {

            if (c == SEPARATOR_CHAR) {

                String token = Strings.clean(sb.toString());

                if (delimiterCount == 0) {
                    base64UrlEncodedHeader = token;
                } else if (delimiterCount == 1) {
                    base64UrlEncodedPayload = token;
                }

                delimiterCount++;
                sb = new StringBuilder(128);
            } else {
                sb.append(c);
            }
        }

        if (delimiterCount != 2) {
            String msg = "JWT strings must contain exactly 2 period characters. Found: " + delimiterCount;
            throw new MalformedJwtException(msg);
        }
        if (sb.length() > 0) {
            base64UrlEncodedDigest = sb.toString();
        }

        if (base64UrlEncodedPayload == null) {
            throw new MalformedJwtException("JWT string '" + jwt + "' is missing a body/payload.");
        }

        // =============== Header =================
        Header header = null;

        if (base64UrlEncodedHeader != null) {
            String origValue = TextCodec.BASE64URL.decodeToString(base64UrlEncodedHeader);
            Map<String, Object> m = readValue(origValue);

            if (base64UrlEncodedDigest != null) {
                header = new DefaultJwsHeader(m);
            } else {
                header = new DefaultHeader(m);
            }
        }

        // =============== Body =================
        String payload = TextCodec.BASE64URL.decodeToString(base64UrlEncodedPayload);

        Claims claims = null;

        if (payload.charAt(0) == '{' && payload.charAt(payload.length() - 1) == '}') { //likely to be json, parse it:
            Map<String, Object> claimsMap = readValue(payload);
            claims = new DefaultClaims(claimsMap);
        }

        // =============== Signature =================
        if (base64UrlEncodedDigest != null) { //it is signed - validate the signature

            JwsHeader jwsHeader = (JwsHeader)header;

            SignatureAlgorithm algorithm = null;

            if (header != null) {
                String alg = jwsHeader.getAlgorithm();
                if (Strings.hasText(alg)) {
                    algorithm = SignatureAlgorithm.forName(alg);
                }
            }

            if (algorithm == null || algorithm == SignatureAlgorithm.NONE) {
                //it is plaintext, but it has a signature.  This is invalid:
                String msg = "JWT string has a digest/signature, but the header does not reference a valid signature " +
                             "algorithm.";
                throw new MalformedJwtException(msg);
            }

            if (key != null && keyBytes != null) {
                throw new IllegalStateException("A key object and key bytes cannot both be specified. Choose either.");
            }

            //digitally signed, let's assert the signature:
            Key key = this.key;

            if (key == null) { //fall back to keyBytes

                if (!Objects.isEmpty(this.keyBytes)) {

                    Assert.isTrue(!algorithm.isRsa(), "Key bytes cannot be specified for RSA signatures.  Please specify a PublicKey or PrivateKey instance.");

                    key = new SecretKeySpec(keyBytes, algorithm.getJcaName());
                }
            }

            Assert.notNull(key, "A signing key must be specified if the specified JWT is digitally signed.");

            //re-create the jwt part without the signature.  This is what needs to be signed for verification:
            String jwtWithoutSignature = base64UrlEncodedHeader + SEPARATOR_CHAR + base64UrlEncodedPayload;

            JwtSignatureValidator validator = new DefaultJwtSignatureValidator(algorithm, key);

            if (!validator.isValid(jwtWithoutSignature, base64UrlEncodedDigest)) {
                String msg = "JWT signature does not match locally computed signature. JWT validity cannot be " +
                             "asserted and should not be trusted.";
                throw new SignatureException(msg);
            }
        }

        Object body = claims != null ? claims : payload;

        if (base64UrlEncodedDigest != null) {
            return new DefaultJws<Object>((JwsHeader)header, body, base64UrlEncodedDigest);
        } else {
            return new DefaultJwt<Object>(header, body);
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readValue(String val) {
        try {
            return objectMapper.readValue(val, Map.class);
        } catch (IOException e) {
            throw new MalformedJwtException("Unable to read JSON value: " + val, e);
        }
    }
}
