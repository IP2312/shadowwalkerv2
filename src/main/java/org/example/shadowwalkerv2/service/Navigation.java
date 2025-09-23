package org.example.shadowwalkerv2.service;


import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class Navigation {
    private final OverpassService overpassService;
    private final MapService mapService;
    private final Util util;
    private static final double EDGE_PENALTY_METERS = 25.0; // tune 10–50


    public Navigation() {

        this.overpassService = new OverpassService();
        this.mapService = new MapService();
        this.util = new Util();

    }
    private static final double INF = Double.POSITIVE_INFINITY;

    private PathResult aStar(long startId,
                             long goalId,
                             Map<Long, List<Long>> adj,
                             Map<Long, RouteNode> nodes,
                             Set<Long> blockedNodes,
                             Set<Edge> blockedEdges,
                             Set<Edge> exploredEdges) {


        if (blockedNodes.contains(startId) || blockedNodes.contains(goalId)) return null;

        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(Comparator.comparingDouble(e -> e.f));
        Map<Long, Double> g = new HashMap<>();
        Map<Long, Double> distance = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        Set<Long> closed = new HashSet<>();

//        for (Long id : nodes.keySet())
//        {
//            //g.put(id, INF);
//            //distance.put(id, INF);
//        }
        g.put(startId, 0.0);
        distance.put(startId, 0.0);
        pq.add(new NodeEntry(startId, 0.0, calculateH(startId, goalId, nodes),0));

        while (!pq.isEmpty()) {
            NodeEntry cur = pq.poll();
            // stale-entry guard if g not equal -> better bath was found not added
            Double best = g.get(cur.id);
            if (best == null || cur.g > best) continue;
            if (!closed.add(cur.id)) continue;

            if (cur.id == goalId) {
                ArrayList<Long> path = new ArrayList<>();
                for (Long x = goalId; x != null; x = parent.get(x)) path.add(x);
                Collections.reverse(path);
                return new PathResult(distance.get(goalId),g.get(goalId),path);
            }

            //get list of neighbours foe current node u startNode v targetNode of Edge
            for (long v : adj.getOrDefault(cur.id, Collections.emptyList())) {
                if (blockedNodes.contains(v)) continue;
                if (blockedEdges.contains(new Edge(cur.id, v))) continue;
                if (closed.contains(v)) continue;

                double base = calculateDistanceBetweenNodes(cur.id, v, nodes);
                double pen  = (exploredEdges != null && exploredEdges.contains(new Edge(cur.id, v)))
                        ? EDGE_PENALTY_METERS : 0.0;

                double totalDistance = distance.get(cur.id) + base;
                double tentative = g.get(cur.id) + base + pen;

                if (tentative < g.getOrDefault(v, INF)) {
                    g.put(v, tentative);
                    distance.put(v, totalDistance);
                    parent.put(v, cur.id);
                    pq.add(new NodeEntry(v, tentative, tentative + calculateH(v, goalId, nodes),totalDistance));
                }
            }
        }
        return null; // unreachable
    }

    public ArrayList<Path> findeKRouts(GeoCoordinate start, GeoCoordinate goal, int K) {
       int nrRouts = 0;
        ArrayList<Path> routes = new ArrayList<>();

        if (K <= 0){
            System.out.println("K <= 0");
            return routes;
        }

        // 1) Load OSM ways/nodes for the corridor between start and goal
        OverpassResponse routElements = overpassService.loadRouts(start, goal);
        ArrayList<RouteNode> routeNodes = new ArrayList<>();
        ArrayList<RoutWay> ways = new ArrayList<>();
        for (OverpassElement e : routElements.getElements()) {
            if ("way".equals(e.type)) {
                ways.add(new RoutWay(e.id, "road", new ArrayList<>(e.nodes)));
            } else if ("node".equals(e.type)) {
                routeNodes.add(new RouteNode(e.id, new GeoCoordinate(e.lat, e.lon)));
            }
        }
        if (routeNodes.isEmpty()){
            System.out.println("No RoutNodes");
            return routes;
        }
        System.out.println("Routs loaded");

        // 2) Index nodes by id
        HashMap<Long, RouteNode> nodesMap = new HashMap<>(routeNodes.size() * 2);
        for (RouteNode n : routeNodes) nodesMap.put(n.getId(), n);

        // 3) Snap start/goal to nearest nodes
        long sId = -1, tId = -1;
        {
            double bestS = Double.POSITIVE_INFINITY, bestT = Double.POSITIVE_INFINITY;
            for (RouteNode n : routeNodes) {
                double ds = mapService.haversineDistance(start, n.getCoordinate());
                if (ds < bestS) {
                    bestS = ds; sId = n.getId();
                }
                double dt = mapService.haversineDistance(goal, n.getCoordinate());
                if (dt < bestT) {
                    bestT = dt; tId = n.getId(); }
            }
            if (sId == -1 || tId == -1) return routes;
        }
        System.out.println("Start/Goal snapped");

        // 4) Build adjacency (finde neighbours for each node)
        Map<Long, List<Long>> adj = buildAdjacency(ways);

        // 5) First shortest path (A*)
        PathResult p1 = aStar(sId, tId, adj, nodesMap, Collections.emptySet(), Collections.emptySet(),Collections.emptySet());
        if (p1 == null) return routes;
        routes.add(util.toPath(p1, nodesMap, nrRouts++));
        if (K == 1) return routes;
        System.out.println("first rout found");

        // 6) Yen's loop
        List<List<Long>> A = new ArrayList<>();     // accepted paths (node ids)
        A.add(p1.pathIds);

        // candidates by cost in pq
        PriorityQueue<PathResult> B = new PriorityQueue<>(Comparator.comparingDouble(c -> c.cost));
        Set<String> seen = new HashSet<>();
        seen.add(signature(p1.pathIds));                // avoid duplicates of P1


        while (A.size() < K) {
            List<Long> prev = A.get(A.size() - 1);// last accepted path
            Set<Edge> penaltyEdges = new HashSet<>();
            for (List<Long> p : A) addEdgesOfPathTo(penaltyEdges, p);

            for (int i = 0; i < prev.size() - 1; i++) {
                long spur = prev.get(i);
                List<Long> root = new ArrayList<>(prev.subList(0, i + 1)); // inclusive of spur

                // Block prefix nodes (except the spur) to keep paths loopless
                Set<Long> blockedNodes = new HashSet<>(root.subList(0, Math.max(0, root.size() - 1)));

                // Block the next edge after this prefix for EVERY accepted path sharing the prefix

                Set<Edge> blockedEdges = new HashSet<>();
                for (List<Long> P : A) {
                    //if P to short or the path shares equal root
                    if (P.size() > i && P.subList(0, i + 1).equals(root)) {
                        blockedEdges.add(new Edge(P.get(i), P.get(i + 1)));
                    }
                }

                PathResult spurRes = aStar(spur, tId, adj, nodesMap, blockedNodes, blockedEdges, penaltyEdges);
                if (spurRes == null){
                    continue;
                }

                // Combine root spur
                List<Long> cand = new ArrayList<>(root);
                cand.remove(cand.size() - 1);
                cand.addAll(spurRes.pathIds);


                String sig = signature(cand);
                if (seen.add(sig)) {

                    Map<String,Double> distanceCost = new HashMap<>(pathDistanceCost(cand, nodesMap, penaltyEdges));
                    double distance = distanceCost.get("distance");
                    double totalCost = distanceCost.get("cost");
                    B.add(new PathResult(distance,totalCost, cand));
                }
            }

            if (B.isEmpty()){
                System.out.println("B empty");
                break;                // no more alternatives
            }
            PathResult best = B.poll();
            A.add(best.pathIds);
            nrRouts++;

            routes.add(util.toPath(best, nodesMap, nrRouts));
            //routes.add(toCoords(best.path, nodesMap));
        }
        System.out.println("K routs found: " + routes.size());
        return routes;
    }


    private record NodeEntry(long id, double g, double f, double distance) {
    }
    private Map<Long, List<Long>> buildAdjacency(List<RoutWay> ways) {
        Map<Long, List<Long>> adj = new HashMap<>();
        for (RoutWay w : ways) {
            List<Long> ids = w.getNodesId();
            for (int i = 0; i < ids.size(); i++) {
                long a = ids.get(i);
                adj.computeIfAbsent(a, k -> new ArrayList<>());
                if (i > 0) adj.get(a).add(ids.get(i - 1));
                if (i < ids.size() - 1) adj.get(a).add(ids.get(i + 1));
            }
        }
        return adj;
    }
    private double calculateDistanceBetweenNodes(long u, long v, Map<Long, RouteNode> nodes) {
        GeoCoordinate cu = nodes.get(u).getCoordinate();
        GeoCoordinate cv = nodes.get(v).getCoordinate();
        return mapService.haversineDistance(cu, cv);
    }
    private double calculateH(long u, long goal, Map<Long, RouteNode> nodes) {
        return calculateDistanceBetweenNodes(u, goal, nodes);
    }


    private String signature(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) sb.append(id).append('-');
        return sb.toString();
    }

    //Penalties
    private void addEdgesOfPathTo(Set<Edge> out, List<Long> ids) {
        for (int i = 0; i + 1 < ids.size(); i++) {
            long u = ids.get(i), v = ids.get(i + 1);
            out.add(new Edge(u, v));
            out.add(new Edge(v, u)); // undirected: penalize both directions
        }
    }


    private Map<String,Double> pathDistanceCost(List<Long> ids, Map<Long, RouteNode> nodes, Set<Edge> penaltyEdges) {
        double distance = 0.0;
        double cost = 0.0;
        for (int i = 0; i + 1 < ids.size(); i++) {
            long u = ids.get(i), v = ids.get(i + 1);
            double base = calculateDistanceBetweenNodes(u, v, nodes);
            double pen  = penaltyEdges.contains(new Edge(u, v)) ? EDGE_PENALTY_METERS : 0.0;
            distance += base;
            cost += base + pen;
        }
        Map<String,Double> out = new HashMap<>();
        out.put("distance", distance);
        out.put("cost", cost);
        return out;
    }




}
