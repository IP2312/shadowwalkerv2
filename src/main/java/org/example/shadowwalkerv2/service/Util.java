package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.model.PathResult;
import org.example.shadowwalkerv2.model.Path;
import org.example.shadowwalkerv2.model.RouteNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Map;

@Service
public class Util {

    public Path toPath(PathResult best, Map<Long, RouteNode> nodes, int pathNr) {
        Path newPath = new Path(pathNr, new LinkedHashSet<>(), best.cost, best.length);

        for (Long id : best.pathIds) {
            newPath.getNodes().add(nodes.get(id));
        }
        return newPath;
    }
}
