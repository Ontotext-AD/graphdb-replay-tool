package com.ontotext.graphdb.replaytool.goreplay;

import java.util.HexFormat;

/**
 * Handles GoReplay package decoding and parsing of the GoReplay header, as well as encoding.
 * @author Copyright &copy; 2024 Ontotext AD
 * @version 1.0
 */
public class GoReplayPackage {

    /** Package is an HTTP request */
    public static final char PACKAGE_TYPE_REQUEST = '1';
    /** Package is an HTTP response from the source instance */
    public static final char PACKAGE_TYPE_RESPONSE = '2';
    /** Package is an HTTP response from the target instance */
    public static final char PACKAGE_TYPE_REPLAY = '3';

    private static final String HTTP_HEADER_END = "\n\n";

    private final String receivedRaw;
    /** Decoded package with header */
    protected String receivedDecoded;
    private boolean modified = false;
    private final HexFormat hex = HexFormat.of();
    private char type;
    private String id;

    private int headerLength;

    private String extractedHeader = null;

    /**
     * Get package from a string
     * <p>
     * Decodes the package payload, identifies type and ID.
     * </p>
     *
     * @param receivedRaw A hex encoded GoReplay package
     */
    public GoReplayPackage(String receivedRaw) {
        this.receivedRaw = receivedRaw;
        parsePackage();
    }

    private void parsePackage() {
        byte[] raw = hex.parseHex(receivedRaw);
        receivedDecoded = new String(raw);
        type = receivedDecoded.charAt(0);
        id = receivedDecoded.substring(2, receivedDecoded.indexOf(" ", 2));
        headerLength = receivedDecoded.indexOf("\n");
    }

    /**
     * Retrieves the length of the GoReplay header
     *
     * @return Length of header
     */
    public int getHeaderLength() {
        return headerLength;
    }

    /**
     * Set the package as modified
     * */
    protected void modify() {
        modified = true;
        extractedHeader = null;
    }

    /**
     * Check of the payload has been modified in some way
     *
     * @return True if the package has been modified
     */
    public boolean modified() {
        return modified;
    }


    /**
     * Type of payload:
     * <UL>
     * <LI>1 - Request</LI>
     * <LI>2 - Captured response</LI>
     * <LI>3 - Response received during replay</LI>
     * </UL>
     *
     * @return Type of payload
     */
    public char getType() {
        return type;
    }

    /**
     * Returns the unique ID that links request/response/replay response
     *
     * @return Request ID
     */
    public String getId() {
        return id;
    }

    /**
     * Encodes the payload in the format required by GoReplay
     *
     * @return Hex encoded payload
     */
    public String getPayload() {
        return modified() ? hex.formatHex(receivedDecoded.getBytes()) : receivedRaw;
    }

    /**
     * Extracts the HTTP header of the request ot response
     *
     * @return HTTP header
     */
    public String getHttpHeader() {
        if (extractedHeader == null) {
            int headerEnd = receivedDecoded.indexOf(HTTP_HEADER_END, headerLength);
            if (headerEnd < 0) {
                headerEnd = receivedDecoded.length() - 1;
            }
            extractedHeader = receivedDecoded.substring(headerLength, headerEnd);
        }
        return extractedHeader;
    }


}
