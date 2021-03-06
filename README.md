[![Build Status](https://travis-ci.org/jwtk/jjwt.svg?branch=master)](https://travis-ci.org/jwtk/jjwt)

# Java JWT: JSON Web Token for Java

JJWT aims to be the easiest to use and understand library for creating and verifying JSON Web Tokens (JWTs) on the JVM.

JJWT is a 'clean room' implementation based solely on the [JWT](https://tools.ietf.org/html/draft-ietf-oauth-json-web-token-25), [JWS](https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-31), [JWE](https://tools.ietf.org/html/draft-ietf-jose-json-web-encryption-31) and [JWA](https://tools.ietf.org/html/draft-ietf-jose-json-web-algorithms-31) RFC draft specifications.

## Installation

Use your favorite Maven-compatible build tool to pull the dependency (and its transitive dependencies) from Maven Central:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.1</version>
</dependency>
```

Note: JJWT depends on Jackson 2.x.  If you're already using an older version of Jackson in your app, [read this](#olderJackson)

## Usage

Most complexity is hidden behind convenient and readable Builder chaining calls.  Here's an example:

```java
import io.jsonwebtoken.Jwts;
import static io.jsonwebtoken.SignatureAlgorithm.*;

//Let's create a random signing key for testing:
Random random = new SecureRandom();
byte[] key = new byte[64];
random.nextBytes(key);

Claims claims = Jwts.claims().setIssuer("Me").setSubject("Joe");

String jwt = Jwts.builder().setClaims(claims).signWith(HS256, key).compact();
```

How easy was that!?

Now let's verify the JWT (you should always discard JWTs that don't match an expected signature):

```java
Token token = Jwts.parser().setSigningKey(key).parse(jwt);

assert token.getClaims().getSubject().equals("Joe");
```

You have to love one-line code snippets in Java!

But what if signature validation failed?  You can catch `SignatureException` and react accordingly:

```java
try {

    Jwts.parser().setSigningKey(key).parse(jwt);

    //OK, we can trust this JWT

} catch (SignatureException e) {

    //don't trust the JWT!
}
```

## Supported Features

* Creating and parsing plaintext compact JWTs

* Creating, parsing and verifying digitally signed compact JWTs (aka JWSs) with the following algorithms:
    * HS256: HMAC using SHA-384
    * HS384: HMAC using SHA-384
    * HS512: HMAC using SHA-512
    * RS256: RSASSA-PKCS-v1_5 using SHA-256
    * RS384: RSASSA-PKCS-v1_5 using SHA-384
    * RS512: RSASSA-PKCS-v1_5 using SHA-512
    * PS256: RSASSA-PSS using SHA-256 and MGF1 with SHA-256
    * PS384: RSASSA-PSS using SHA-384 and MGF1 with SHA-384
    * PS512: RSASSA-PSS using SHA-512 and MGF1 with SHA-512

## Currently Unsupported Features

* [Non-compact](https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-31#section-7.2) serialization and parsing.
* Elliptic Curve signature algorithms `ES256`, `ES384` and `ES512`.
* JWE (Encryption for JWT)

These feature sets will be implemented in a future release when possible.  Community contributions are welcome!

<a name="olderJackson"></a>
#### Already using an older Jackson dependency?

JJWT depends on Jackson 2.4.x (or later).  If you are already using a Jackson version in your own application less than 2.x, for example 1.9.x, you will likely see [runtime errors](https://github.com/jwtk/jjwt/issues/1).  To avoid this, you should change your project build configuration to explicitly point to a 2.x version of Jackson.  For example:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.4.2</version>
</dependency>
```
