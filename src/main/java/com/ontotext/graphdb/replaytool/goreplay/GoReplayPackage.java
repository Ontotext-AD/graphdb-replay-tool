package com.ontotext.graphdb.replaytool.goreplay;

import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @copyright Copyright &copy; 2024 Ontotext
 * @versiom 1.0
 */
public class GoReplayPackage {

    static Pattern TRANSACTION_MATCH_REQUEST = Pattern.compile("/transactions/([0-9a-f\\-]+) HTTP", Pattern.CASE_INSENSITIVE);
    static Pattern TRANSACTION_MATCH_HEADER = Pattern.compile("Location: .*/transactions/([0-9a-f\\-]+)", Pattern.CASE_INSENSITIVE);

    final private String receivedRaw;
    private String receivedDecoded;
    private boolean modified = false;
    final private HexFormat hex = HexFormat.of();
    private char type;
    private String id;
    private String compoundTransaction = null;
    private int headerLength;
    private boolean transactionMatched = false;

    /**
     * Get package from a string
     * <p>
     *     Decodes the package payload, identifies type and ID.
     * </p>
     * @param receivedRaw
     */
    public GoReplayPackage(String receivedRaw) {
        this.receivedRaw = receivedRaw;
        parsePackage();

    }

    /**
     * Get package from a open stream Scanner
     * <p>
     *     Decodes the package payload identifies type and ID
     * </p>
     * @param stream
     * @throws NoSuchElementException
     * @throws InterruptedException
     */
    public GoReplayPackage(Scanner stream) throws NoSuchElementException, InterruptedException {
        receivedRaw = stream.nextLine();
        parsePackage();
    }

    private void parsePackage() {
        byte[] raw = hex.parseHex(receivedRaw);
        receivedDecoded = new String(raw);
        type = receivedDecoded.charAt(0);
        id = receivedDecoded.substring(2, receivedDecoded.indexOf(" ", 2));
        headerLength = receivedDecoded.indexOf("\n");
    }

    private void modify() {
        modified = true;
    }

    /**
     * Check of the payload has been modified in some way
     * @return True if the package has been modified
     */
    public boolean modified() {
        return modified;
    }

    private boolean findCompoundTransaction() {
        String matchArea;
        Matcher matcher;
        if (transactionMatched) {
            return compoundTransaction != null;
        } else {
            transactionMatched = true;
        }
        if (type == '1') {
            // Request package transaction ID is in the first line of the request
            matchArea = receivedDecoded.substring(headerLength - 1, receivedDecoded.indexOf("\n", headerLength + 1) - headerLength);
            matcher = TRANSACTION_MATCH_REQUEST.matcher(matchArea);
        } else {
            // Response package, transaction ID is in the Location header
            matchArea = receivedDecoded.substring(headerLength);
            matcher = TRANSACTION_MATCH_HEADER.matcher(matchArea);
        }
        if (matcher.find()) {
            compoundTransaction = matcher.group(1);
            return true;
        }
        return false;
    }

    /**
     * Check if the package references a GraphDB compound transaction in the URL for requests and in a Location header
     * for responces.
     * @return True if a compoind transaction ID has been identified
     */
    public boolean isCompoundTransaction() {
        return findCompoundTransaction();
    }

    /**
     * Returns the compound transaction ID or null if compound transaction cannot be identified.
     * @return Transaction ID
     */
    public String getCompoundTransaction() {
        findCompoundTransaction();
        return compoundTransaction;
    }

    /**
     * Type of payload:
     * <UL>
     *     <LI>1 - Request</LI>
     *     <LI>2 - Captured response</LI>
     *     <LI>3 - Response received during replay</LI>
     * </UL>
     * @return Type of payload
     */
    public char getType() {
        return type;
    }

    /**
     * Returns the uniqie ID that links request/response/replay response
     * @return Request ID
     */
    public String getId() {
        return id;
    }

    /**
     * Encodes the payload in the format required by GoReplay
     * @return
     */
    public String getPayload() {
        return modified() ? hex.formatHex(receivedDecoded.getBytes()) : receivedRaw;
    }

    /**
     * Replace the compound transaction ID in the package.
     * @param newCompoundTransaction
     */
    public void replaceCompoundTransaction(String newCompoundTransaction) {
        if (!isCompoundTransaction() || newCompoundTransaction == null ||
                newCompoundTransaction.equals(compoundTransaction)) {
            return;
        }
        modify();
        receivedDecoded = receivedDecoded.replace(compoundTransaction, newCompoundTransaction);
        compoundTransaction = newCompoundTransaction;
    }
}
