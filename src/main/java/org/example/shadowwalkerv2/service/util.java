package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.model.Path;
import org.example.shadowwalkerv2.model.RouteNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class util {

    public Path toPath(List<Long> ids, Map<Long, RouteNode> nodes, int pathNr) {
        Path newPath = new Path(pathNr, null, 0);

        return newPath;
    }
}
