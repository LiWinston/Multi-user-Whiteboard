package Service;

import io.grpc.*;
import io.jsonwebtoken.Claims;

public class AuthInterceptor implements ServerInterceptor {
    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String authorizationHeader = headers.get(AUTHORIZATION_METADATA_KEY);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Authorization token is missing or invalid"), headers);
            return new ServerCall.Listener<>() {};
        }

        String token = authorizationHeader.substring("Bearer ".length());
        try {
            Claims claims = validateToken(token);
//            Context context = Context.current().withValue(Constants.USER_CONTEXT_KEY, claims.getSubject());
//            return Contexts.interceptCall(context, call, headers, next);
            return null;
        } catch (Exception e) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token: " + e.getMessage()), headers);
            return new ServerCall.Listener<>() {};
        }
    }

    private Claims validateToken(String token) throws Exception {
        // Assuming the signing key is "secretKey"
        return null;
    }
}
