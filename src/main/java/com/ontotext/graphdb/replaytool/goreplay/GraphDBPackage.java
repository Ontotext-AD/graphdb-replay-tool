package com.ontotext.graphdb.replaytool.goreplay;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class GraphDBPackage extends RDF4JPackage {

    static Pattern AUTHORIZATION_TOKEN = Pattern.compile("Authorization: ((\\w+) (\\S+))", Pattern.CASE_INSENSITIVE);

    static public final String AUTHORIZATION_TYPE_NONE = "NONE";
    static public final String AUTHORIZATION_TYPE_BASIC = "basic";
    static public final String AUTHORIZATION_TYPE_GDB = "gdb";

    private static final String HMAC_ALGO = "HmacSHA256";

    private String authorizationToken = null;
    private String authorizationType = null;
    private String authorizationValue = null;
    private String authorizationUsername = null;

    private static final Map<String, String> authorizationTokenCache = new HashMap<>();

    private static Mac hmac;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads a package and parses it for authentication
     *
     * @see GoReplayPackage(Scanner)
     */
    public GraphDBPackage(Scanner stream) throws NoSuchElementException, InterruptedException {
        super(stream);
        parseAuthorizationToken();
    }

    /**
     * Processes a hex encoded GoReplay package and parses it for authentication
     *
     * @see GoReplayPackage(String)
     */
    public GraphDBPackage(String receivedRaw) {
        super(receivedRaw);
        parseAuthorizationToken();
    }

    private void parseAuthorizationToken() {
        Matcher matcher = AUTHORIZATION_TOKEN.matcher(getHttpHeader());
        if (matcher.find()) {
            authorizationValue = matcher.group(1);
            authorizationType = matcher.group(2).toLowerCase();
            if (authorizationType.isEmpty() ||
                    !(AUTHORIZATION_TYPE_BASIC.equals(authorizationType) ||
                            AUTHORIZATION_TYPE_GDB.equals(authorizationType))) {
                authorizationType = AUTHORIZATION_TYPE_NONE;
            } else {
                authorizationToken = matcher.group(3);
            }
            if (AUTHORIZATION_TYPE_BASIC.equals(getAuthorizationType())) {
                String token = getAuthorizationToken();
                String decoded = new String(Base64.getDecoder().decode(token));
                authorizationUsername = decoded.substring(0, decoded.indexOf(":"));
            }
            if (AUTHORIZATION_TYPE_GDB.equals(getAuthorizationType())) {
                String token = getAuthorizationToken();
                String tokenPayload = new String(Base64.getDecoder().decode(token.substring(0, token.indexOf(".") )));
                try {
                    TypeReference<HashMap<String,Object>> hashMapTypeReference = new TypeReference<>() {};
                    HashMap<String, Object> userInfo = objectMapper.readValue(tokenPayload, hashMapTypeReference);
                    authorizationUsername = (String) userInfo.get("username");
                } catch (JsonProcessingException e) {
                    authorizationType = AUTHORIZATION_TYPE_NONE;
                }

            }
        } else {
            authorizationType = AUTHORIZATION_TYPE_NONE;
        }
    }

    /**
     * Returns True if GraphDB or Basic authentication token is present in the package
     *
     * @return True if authentication token is present
     */
    public Boolean usesAuthorization() {
        return !AUTHORIZATION_TYPE_NONE.equals(authorizationType);
    }

    /**
     * The authorization token without the authorization schema
     *
     * @return Authorization token
     */
    public String getAuthorizationToken() {
        return authorizationToken;
    }

    /**
     * Authorization type, one of:
     * <UL>
     * <LI>AUTHORIZATION_TYPE_NONE</LI>
     * <LI>AUTHORIZATION_TYPE_GDB</LI>
     * <LI>AUTHORIZATION_TYPE_BASIC</LI>
     * </UL>
     * <P>Digest is currently not supported</P>
     *
     * @return Authorization type
     */
    public String getAuthorizationType() {
        return authorizationType;
    }

    /**
     * The authorization username
     *
     * @return Username
     */
    public String getAuthorizationUser() {
        return authorizationUsername;
    }

    /**
     * Set the secret used to generate new authentication tokens
     * <P>This method must be called before calls before calls to replaceAuthorizationToken,
     * or an InvalidKeyException will be thrown.</P>
     *
     * @param hmacSecret Secret must be the same as the secret set on the server being replayed to
     * @throws NoSuchAlgorithmException Unlikely to be thrown
     * @throws InvalidKeyException      Unlikely to be thrown
     */
    public static void setAuthorizationSecret(String hmacSecret) throws NoSuchAlgorithmException, InvalidKeyException {
        if (hmacSecret == null || hmacSecret.isEmpty()) return;
        hmac = Mac.getInstance(HMAC_ALGO);
        hmac.init(new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(hmacSecret.getBytes()), HMAC_ALGO));
    }

    private String createToken(String user) throws InvalidKeyException {
        if (hmac == null) {
            throw new InvalidKeyException("HMAC not initialized");
        }
        HashMap<String,Object> userInfo = new HashMap<>();
        userInfo.put("username", user);
        userInfo.put("authenticatedAt", System.currentTimeMillis());
        try {
            String tokenPayload = objectMapper.writeValueAsString(userInfo);
            return Base64.getEncoder().encodeToString(tokenPayload.getBytes()) + "." +
                    Base64.getEncoder().encodeToString(hmac.doFinal(tokenPayload.getBytes()));
        } catch (JsonProcessingException e) {
            return authorizationValue;
        }
    }

    /**
     * Replace authorization token with a newly generated token for the same user
     *
     * @throws InvalidKeyException Thrown if the HMAC is not initialized
     */
    public void replaceAuthorizationToken() throws InvalidKeyException {
        if (!usesAuthorization()) return;
        final String user = getAuthorizationUser();
        if (user != null) {
            String token;
            if (authorizationTokenCache.containsKey(user)) {
                token = authorizationTokenCache.get(user);
            } else {
                token = createToken(user);
                authorizationTokenCache.put(user, token);
            }
            modify();
            final String newAuthorizationValue = "GDB " + token;
            receivedDecoded = receivedDecoded.replace(authorizationValue, newAuthorizationValue);
            authorizationValue = newAuthorizationValue;
            authorizationType = AUTHORIZATION_TYPE_GDB;
            authorizationToken = token;
        }
    }
}
