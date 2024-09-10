package com.ontotext.graphdb.replaytool.goreplay;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RDF4JPackage extends GoReplayPackage {


    static Pattern TRANSACTION_MATCH_REQUEST = Pattern.compile("/transactions/([0-9a-f\\-]+) HTTP", Pattern.CASE_INSENSITIVE);
    static Pattern TRANSACTION_MATCH_HEADER = Pattern.compile("Location: .*/transactions/([0-9a-f\\-]+)", Pattern.CASE_INSENSITIVE);

    private String compoundTransaction = null;
    private boolean transactionMatched = false;

    /**
     * @see GoReplayPackage(Scanner)
     */
    public RDF4JPackage(Scanner stream) throws NoSuchElementException {
        super(stream);
    }

    /**
     * @see GoReplayPackage(String)
     */
    public RDF4JPackage(String receivedRaw) {
        super(receivedRaw);
    }

    private boolean findCompoundTransaction() {
        String matchArea;
        Matcher matcher;
        if (transactionMatched) {
            return compoundTransaction != null;
        } else {
            transactionMatched = true;
        }
        if (getType() == '1') {
            // Request package transaction ID is in the first line of the request
            matchArea = receivedDecoded.substring(getHeaderLength() - 1,
                    receivedDecoded.indexOf("\n", getHeaderLength() + 1) - getHeaderLength());
            matcher = TRANSACTION_MATCH_REQUEST.matcher(matchArea);
        } else {
            // Response package, transaction ID is in the Location header
            matchArea = receivedDecoded.substring(getHeaderLength());
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
     *
     * @return True if a compoind transaction ID has been identified
     */
    public boolean isCompoundTransaction() {
        return findCompoundTransaction();
    }

    /**
     * Returns the compound transaction ID or null if compound transaction cannot be identified.
     *
     * @return Transaction ID
     */
    public String getCompoundTransaction() {
        findCompoundTransaction();
        return compoundTransaction;
    }

    /**
     * Replace the compound transaction ID in the package.
     *
     * @param newCompoundTransaction New transaction ID
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
