package com.ontotext.graphdb.replaytool.goreplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class GoReplayMiddleware {

    AtomicBoolean inOperation = new AtomicBoolean(true);

    HashMap<String, String> transactionMap = new HashMap<>();
    HashMap<String, String> byIdPool = new HashMap<>();
    List<GoReplayPackage> reprocessQueue = new ArrayList<>();

    public GoReplayMiddleware(){
        Thread thread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            inOperation.set(false);
            thread.interrupt();
        }));
    }

    public void run(){
        Scanner scanner = new Scanner(System.in);
        while (inOperation.get()) {
            try {
                GoReplayPackage pkg = null;
                if(!reprocessQueue.isEmpty()){
                    for (GoReplayPackage reprocessPackage : reprocessQueue) {
                        if(transactionMap.containsKey(reprocessPackage.getCompoundTransaction())){
                            pkg = reprocessPackage;
                            reprocessQueue.remove(reprocessPackage);
                            break;
                        }
                    }
                }
                if(pkg == null){
                    pkg = new GoReplayPackage(scanner);
                }
                if(pkg.getType() == '1'){
                    if(pkg.isCompoundTransaction()){
                        if(transactionMap.containsKey(pkg.getCompoundTransaction())){
                            pkg.replaceCompoundTransaction(transactionMap.get(pkg.getCompoundTransaction()));
                        } else {
                            reprocessQueue.add(pkg);
                            continue;
                        }
                    }
                } else {
                    if(pkg.isCompoundTransaction()){
                        String id = pkg.getId();
                        if(byIdPool.containsKey(id)){
                            if(pkg.getType() == '2'){
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
                System.out.println(pkg.getPayload());
            } catch (InterruptedException ignored){}
        }
    }
}
