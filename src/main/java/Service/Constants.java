package Service;

import io.grpc.Context;
import io.grpc.Metadata;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

//grpc 多端公用的常量
public class Constants {
    private static final String JWT_SIGNING_KEY = "L8hHXsaQOUjk5rg7XPGv4eL36anlCrkMz8CJ0i/8E/0=";
    public static SecretKey JWT_KEY = Keys.hmacShaKeyFor(JWT_SIGNING_KEY.getBytes());
    public static final String BEARER_TYPE = "Bearer";

    public static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER);
    public static final Context.Key<String> CLIENT_ID_CONTEXT_KEY = Context.key("clientId");

    private Constants() {
        throw new AssertionError();
    }
}