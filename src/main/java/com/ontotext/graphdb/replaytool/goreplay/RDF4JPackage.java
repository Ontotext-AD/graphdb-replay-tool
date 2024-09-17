package com.ontotext.graphdb.replaytool.goreplay;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles RDF4J specifics: Detects and replaces compound transaction IDs
 * @author Copyright &copy; 2024 Ontotext AD
 * @version 1.0
 */
public class RDF4JPackage extends GoReplayPackage {


    static final Pattern TRANSACTION_MATCH_REQUEST = Pattern.compile("/transactions/([0-9a-fA-Z\\-]+)((\\s)|(\\?.*))HTTP", Pattern.CASE_INSENSITIVE);
    static final Pattern TRANSACTION_MATCH_HEADER = Pattern.compile("Location: .*/transactions/([0-9a-fA-Z\\-]+)", Pattern.CASE_INSENSITIVE);

    private String compoundTransaction = null;
    private boolean transactionMatched = false;

    /**
     * Construct a RDF4J Package instance from a stream
     *
     * @param stream Scanner to read the next line from
     * @throws NoSuchElementException when nothing is available to read
     * @see GoReplayPackage#GoReplayPackage(Scanner)
     */
    public RDF4JPackage(Scanner stream) throws NoSuchElementException {
        super(stream);
    }

    /**
     * Construct a RDF4J Package instance from string
     *
     * @param receivedRaw Raw GoReplay package
     * @see GoReplayPackage#GoReplayPackage(String)
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
        if (getType() == PACKAGE_TYPE_REQUEST) {
            // Request package transaction ID is in the first line of the request
            matchArea = receivedDecoded.substring(getHeaderLength() - 1,
                    receivedDecoded.indexOf("\n", getHeaderLength() + 1));
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
     * for responses.
     *
     * @return True if a compound transaction ID has been identified
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
