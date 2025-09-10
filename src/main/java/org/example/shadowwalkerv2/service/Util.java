package org.example.shadowwalkerv2.service;

import org.example.shadowwalkerv2.model.Path;
import org.example.shadowwalkerv2.model.RouteNode;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class Util {

    public Path toPath(List<Long> ids, Map<Long, RouteNode> nodes, int pathNr) {
        Path newPath = new Path(pathNr, new LinkedHashSet<>(), 0);

        for (Long id : ids) {
            newPath.getNodes().add(nodes.get(id));
        }
        return newPath;
    }
}
