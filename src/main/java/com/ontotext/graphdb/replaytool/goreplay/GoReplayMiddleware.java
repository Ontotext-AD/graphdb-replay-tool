package com.ontotext.graphdb.replaytool.goreplay;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Middleware implementation for GoReplay to handle GraphDB traffic
 *
 * @author Copyright &copy; 2024 Ontotext AD
 * @version 1.0
 */
public class GoReplayMiddleware {

    final AtomicBoolean inOperation = new AtomicBoolean(true);

    final HashMap<String, String> transactionMap = new HashMap<>();
    final HashMap<String, String> byIdPool = new HashMap<>();
    final List<GraphDBPackage> reprocessQueue = new ArrayList<>();
    private Boolean processAuthorization = false;

    /**
     * Installs a shutdown hook and initializes the authentication engine
     */
    public GoReplayMiddleware() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> inOperation.set(false)));
        String authorizationSecret = System.getProperty("authorization.secret");
        if (authorizationSecret != null) {
            try {
                GraphDBPackage.setAuthorizationSecret(authorizationSecret);
                processAuthorization = true;
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Main middleware loop
     */
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (inOperation.get()) {
            GraphDBPackage pkg = null;
            if (!reprocessQueue.isEmpty()) {
                for (GraphDBPackage reprocessPackage : reprocessQueue) {
                    if (transactionMap.containsKey(reprocessPackage.getCompoundTransaction())) {
                        pkg = reprocessPackage;
                        reprocessQueue.remove(reprocessPackage);
                        break;
                    }
                }
            }
            if (pkg == null) {
                String receivedRaw = scanner.nextLine();
                pkg = new GraphDBPackage(receivedRaw);
            }
            if (pkg.getType() == '1') {
                if (pkg.isCompoundTransaction()) {
                    if (transactionMap.containsKey(pkg.getCompoundTransaction())) {
                        pkg.replaceCompoundTransaction(transactionMap.get(pkg.getCompoundTransaction()));
                    } else {
                        reprocessQueue.add(pkg);
                        continue;
                    }
                }
            } else {
                if (pkg.isCompoundTransaction()) {
                    String id = pkg.getId();
                    if (byIdPool.containsKey(id)) {
                        if (pkg.getType() == '2') {
                            transactionMap.put(pkg.getCompoundTransaction(), byIdPool.get(id));
                        } else {
                            transactionMap.put(byIdPool.get(id), pkg.getCompoundTransaction());
                        }
                        byIdPool.remove(id);
                    } else {
                        byIdPool.put(id, pkg.getCompoundTransaction());
                    }
                }
            }
            if (processAuthorization && pkg.usesAuthorization()) {
                try {
                    pkg.replaceAuthorizationToken();
                } catch (InvalidKeyException ignore) {}
            }
            System.out.println(pkg.getPayload());
        }
    }
}
