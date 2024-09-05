package com.ontotext.graphdb.replaytool;

import com.ontotext.graphdb.replaytool.goreplay.GoReplayMiddleware;

public class Main {
    public static void main(String[] args) {
        GoReplayMiddleware middleware = new GoReplayMiddleware();
        middleware.run();
    }
}