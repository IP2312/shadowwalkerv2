package org.example.shadowwalkerv2.service;


import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.*;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

@Service
public class Navigation {
    private final OverpassService overpassService;
    private final MapService mapService;
    private final Frontier frontier;
    private final SunService sunService;

    public Navigation(SunService sunService) {
        this.sunService = sunService;
        this.overpassService = new OverpassService();
        this.mapService = new MapService();
        this.frontier = new Frontier();

    }

    public ArrayList<ArrayList<GeoCoordinate>> findeRoutes(GeoCoordinate start, GeoCoordinate goal) {
        System.out.println("Start:");
        ZonedDateTime time = ZonedDateTime.now().minusHours(1);

        ArrayList<ArrayList<GeoCoordinate>> routes = new ArrayList<>();

        OverpassResponse routElements = overpassService.loadRouts(start, goal);
        ArrayList<RouteNode> routeNodes = new ArrayList<>();
        ArrayList<RoutWay> routs = new ArrayList<>();
        for (OverpassElement element : routElements.getElements()) {
            if ("way".equals(element.type)) {
                routs.add(new RoutWay(element.id, "road", new ArrayList<>(element.nodes))); // FIX
            } else if ("node".equals(element.type)) {
                routeNodes.add(new RouteNode(element.id, new GeoCoordinate(element.lat, element.lon)));
            }
        }
        System.out.println("Routs loaded");

        OverpassResponse buildingElements = overpassService.loadBuildings(start, goal);
        LinkedHashSet<BuildingNode> buildingNodes = new LinkedHashSet<>();
        ArrayList<BuildingWay> buildings = new ArrayList<>();
        for (OverpassElement element : buildingElements.getElements()) {
            if ("way".equals(element.type)) {
                BuildingWay newBuilding = new BuildingWay(element.id, "building", new ArrayList<>(element.nodes)); // FIX
                if (element.tags != null) {
                    newBuilding.setHeight(element.tags.get("height"));
                    newBuilding.setLevels(element.tags.get("building:levels"));
                }
                buildings.add(newBuilding);
            } else if ("node".equals(element.type)) {
                buildingNodes.add(new BuildingNode(element.id, new GeoCoordinate(element.lat, element.lon)));
            }
        }
        System.out.println("buildings loaded");


        if (routeNodes.isEmpty()) {
            System.out.println("No RoutNodes");
            return routes;
        }

        HashMap<Long, RouteNode> nodesMap = new HashMap<>(routeNodes.size() * 2);
        for (RouteNode n : routeNodes) {
            n.setCostToReachNode(Double.POSITIVE_INFINITY);
            n.setEstimatedCostToGoal(0.0);
            n.setParentNode(null);
            n.setExplored(false);
            n.setTotalCount(0);
            n.setShadedCount(0);
            nodesMap.put(n.getId(), n);
        }

        RouteNode startNode = getClosestNode(start, routeNodes);
        RouteNode goalNode = getClosestNode(goal, routeNodes);
        if (startNode == null || goalNode == null) {
            System.out.println("No start or goal Node");
            return routes;
        }

        frontier.clear();
        startNode.setCostToReachNode(0.0);
        startNode.setEstimatedCostToGoal(mapService.haversineDistance(startNode.getCoordinate(), goalNode.getCoordinate()));
        frontier.addOrUpdateNode(startNode);


        ArrayList<RouteNode> shortestPathNodes = null;

        while (!frontier.isEmpty()) {
            RouteNode currentNode = frontier.removeNode();
            if (currentNode == null) {
                System.out.println("currentNode null");
                break;
            }

            if (currentNode.equals(goalNode)) {
                System.out.println("Reconstruct Path");
                shortestPathNodes = reconstructPath(currentNode);
                ArrayList<GeoCoordinate> routCoordinates = new ArrayList<>();
                for (RouteNode n : shortestPathNodes) {
                    routCoordinates.add(n.getCoordinate());
                }
                routes.add(routCoordinates);
                System.out.println("first rout found");
                break;
            }
            // Expand neighbors
            ArrayList<RoutWay> possibleRouts = getRoutsFromNode(currentNode, routs);
            LinkedHashSet<Long> neighbourIds = findNeighboursId(currentNode, possibleRouts);

            for (Long neighbourId : neighbourIds) {
                RouteNode neighbour = nodesMap.get(neighbourId);
                if (neighbour == null) {
                    System.out.println("neighbour null");
                    continue;
                }

                if (currentNode.getParentNode() != null && currentNode.getParentNode().equals(neighbour)) {
                    //System.out.println("Parent == neighbour");
                    continue;
                }
                double edge = mapService.haversineDistance(currentNode.getCoordinate(), neighbour.getCoordinate());
                double tentativeG = currentNode.getCostToReachNode() + edge;

                if (tentativeG < neighbour.getCostToReachNode()) {
                    neighbour.setParentNode(currentNode);
                    neighbour.setCostToReachNode(tentativeG);
                    neighbour.setEstimatedCostToGoal(mapService.haversineDistance(neighbour.getCoordinate(), goalNode.getCoordinate())
                    );
                    frontier.addOrUpdateNode(neighbour);
                }
            }

        }


        System.out.println("returning routs");
        return routes;
    }

    public RouteNode getClosestNode(GeoCoordinate coordinate, ArrayList<RouteNode> nodes) {

        double distance = Double.MAX_VALUE;
        RouteNode currentNode = null;
        for (RouteNode node : nodes) {
            if (distance > mapService.haversineDistance(coordinate, node.getCoordinate())) {
                distance = mapService.haversineDistance(coordinate, node.getCoordinate());
                currentNode = node;
            }
        }
        return currentNode;
    }

    private ArrayList<RouteNode> reconstructPath(RouteNode goal) {
        ArrayList<RouteNode> path = new ArrayList<>();
        for (RouteNode n = goal; n != null; n = n.getParentNode()) {
            path.add(n); // prepend
        }
        Collections.reverse(path);
        return path;
    }

    public ArrayList<RoutWay> getRoutsFromNode(RouteNode node, ArrayList<RoutWay> routs) {
        ArrayList<RoutWay> newRouts = new ArrayList<>();
        for (RoutWay rout : routs) {
            for (Long nodeId : rout.getNodesId()) {
                if (nodeId.equals(node.getId())) {
                    newRouts.add(rout);
                    break; // small perf win
                }
            }
        }
        return newRouts;
    }
    public LinkedHashSet<Long> findNeighboursId(RouteNode currentNode, ArrayList<RoutWay> possibleRouts) {
        LinkedHashSet<Long> neighboursId = new LinkedHashSet<>();
        for (RoutWay rout : possibleRouts) {
            List<Long> ids = rout.getNodesId();
            for (int i = 0; i < ids.size(); i++) {
                if (ids.get(i).equals(currentNode.getId())) { // FIX
                    if (i > 0) neighboursId.add(ids.get(i - 1));
                    if (i < ids.size() - 1) neighboursId.add(ids.get(i + 1));
                }
            }
        }
        return neighboursId;
    }



    // Paste these inside your Navigation class (alongside your other methods/fields)

///////////////////////////////
// Helpers for Yen + A*
    ///////////////////////////////
    private static final class Edge {
        final long u, v;
        Edge(long u, long v) { this.u = u; this.v = v; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge e)) return false;
            return u == e.u && v == e.v;
        }
        @Override public int hashCode() { return Objects.hash(u, v); }
    }
    private static final class NodeEntry {
        final long id; final double g; final double f;
        NodeEntry(long id, double g, double f) { this.id = id; this.g = g; this.f = f; }
    }
    private static final class PathResult {
        final List<Long> ids; final double cost;
        PathResult(List<Long> ids, double cost) { this.ids = ids; this.cost = cost; }
    }
    private static final class Candidate {
        final double cost; final List<Long> path;
        Candidate(double cost, List<Long> path) { this.cost = cost; this.path = path; }
    }
    private static final double INF = Double.POSITIVE_INFINITY;

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
    private double w(long u, long v, Map<Long, RouteNode> nodes) {
        GeoCoordinate cu = nodes.get(u).getCoordinate();
        GeoCoordinate cv = nodes.get(v).getCoordinate();
        return mapService.haversineDistance(cu, cv);
    }
    private double h(long u, long goal, Map<Long, RouteNode> nodes) { return w(u, goal, nodes); }
    private double pathCost(List<Long> ids, Map<Long, RouteNode> nodes) {
        double c = 0.0;
        for (int i = 0; i + 1 < ids.size(); i++) c += w(ids.get(i), ids.get(i + 1), nodes);
        return c;
    }
    private ArrayList<GeoCoordinate> toCoords(List<Long> ids, Map<Long, RouteNode> nodes) {
        ArrayList<GeoCoordinate> out = new ArrayList<>(ids.size());
        for (Long id : ids) out.add(nodes.get(id).getCoordinate());
        return out;
    }
    private String signature(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) sb.append(id).append('-');
        return sb.toString();
    }

    // Local A* with temporary blocks (distance-only). Uses PQ with stale-skip.
    private PathResult aStar(long startId,
                             long goalId,
                             Map<Long, List<Long>> adj,
                             Map<Long, RouteNode> nodes,
                             Set<Long> blockedNodes,
                             Set<Edge> blockedEdges) {
        if (blockedNodes.contains(startId) || blockedNodes.contains(goalId)) return null;

        PriorityQueue<NodeEntry> pq = new PriorityQueue<>(Comparator.comparingDouble(e -> e.f));
        Map<Long, Double> g = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        Set<Long> closed = new HashSet<>();

        for (Long id : nodes.keySet()) g.put(id, INF);
        g.put(startId, 0.0);
        pq.add(new NodeEntry(startId, 0.0, h(startId, goalId, nodes)));

        while (!pq.isEmpty()) {
            NodeEntry cur = pq.poll();
            // stale-entry guard
            if (!Objects.equals(cur.g, g.get(cur.id))) continue;
            if (!closed.add(cur.id)) continue;

            if (cur.id == goalId) {
                ArrayList<Long> path = new ArrayList<>();
                for (Long x = goalId; x != null; x = parent.get(x)) path.add(x);
                Collections.reverse(path);
                return new PathResult(path, g.get(goalId));
            }

            for (long v : adj.getOrDefault(cur.id, Collections.emptyList())) {
                if (blockedNodes.contains(v)) continue;
                if (blockedEdges.contains(new Edge(cur.id, v))) continue;
                if (closed.contains(v)) continue;

                double tentative = g.get(cur.id) + w(cur.id, v, nodes);
                if (tentative < g.getOrDefault(v, INF)) {
                    g.put(v, tentative);
                    parent.put(v, cur.id);
                    pq.add(new NodeEntry(v, tentative, tentative + h(v, goalId, nodes)));
                }
            }
        }
        return null; // unreachable
    }

/////////////////////////////////////
// The method you asked for:
    /////////////////////////////////////
    public ArrayList<ArrayList<GeoCoordinate>> findeKRouts(GeoCoordinate start,
                                                           GeoCoordinate goal,
                                                           int K) {
        ArrayList<ArrayList<GeoCoordinate>> routes = new ArrayList<>();
        if (K <= 0) return routes;

        // 1) Load OSM ways/nodes for the corridor between start & goal
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
        if (routeNodes.isEmpty()) return routes;

        // 2) Index nodes by id
        HashMap<Long, RouteNode> nodesMap = new HashMap<>(routeNodes.size() * 2);
        for (RouteNode n : routeNodes) nodesMap.put(n.getId(), n);

        // 3) Snap start/goal to nearest OSM nodes
        long sId = -1, tId = -1;
        {
            double bestS = Double.POSITIVE_INFINITY, bestT = Double.POSITIVE_INFINITY;
            for (RouteNode n : routeNodes) {
                double ds = mapService.haversineDistance(start, n.getCoordinate());
                if (ds < bestS) { bestS = ds; sId = n.getId(); }
                double dt = mapService.haversineDistance(goal, n.getCoordinate());
                if (dt < bestT) { bestT = dt; tId = n.getId(); }
            }
            if (sId == -1 || tId == -1) return routes;
        }

        // 4) Build adjacency
        Map<Long, List<Long>> adj = buildAdjacency(ways);

        // 5) First shortest path (A*)
        PathResult p1 = aStar(sId, tId, adj, nodesMap, Collections.emptySet(), Collections.emptySet());
        if (p1 == null) return routes;
        routes.add(toCoords(p1.ids, nodesMap));
        if (K == 1) return routes;

        // 6) Yen's loop
        List<List<Long>> A = new ArrayList<>();     // accepted paths (node ids)
        A.add(p1.ids);

        PriorityQueue<Candidate> B = new PriorityQueue<>(Comparator.comparingDouble(c -> c.cost));
        Set<String> seen = new HashSet<>();
        seen.add(signature(p1.ids));                // avoid duplicates of P1

        while (A.size() < K) {
            List<Long> prev = A.get(A.size() - 1);  // last accepted path

            for (int i = 0; i < prev.size() - 1; i++) {
                long spur = prev.get(i);
                List<Long> root = new ArrayList<>(prev.subList(0, i + 1)); // inclusive of spur

                // Block prefix nodes (except the spur) to keep paths loopless
                Set<Long> blockedNodes = new HashSet<>(root.subList(0, Math.max(0, root.size() - 1)));

                // Block the next edge after this prefix for EVERY accepted path sharing the prefix
                Set<Edge> blockedEdges = new HashSet<>();
                for (List<Long> P : A) {
                    if (P.size() > i && P.subList(0, i + 1).equals(root)) {
                        blockedEdges.add(new Edge(P.get(i), P.get(i + 1)));
                    }
                }

                PathResult spurRes = aStar(spur, tId, adj, nodesMap, blockedNodes, blockedEdges);
                if (spurRes == null) continue;

                // Combine root ⊕ spur (avoid duplicating the spur node)
                List<Long> cand = new ArrayList<>(root);
                cand.remove(cand.size() - 1);
                cand.addAll(spurRes.ids);

                String sig = signature(cand);
                if (seen.add(sig)) {
                    double total = pathCost(root, nodesMap) + spurRes.cost;
                    B.add(new Candidate(total, cand));
                }
            }

            if (B.isEmpty()) break;                // no more alternatives
            Candidate best = B.poll();
            A.add(best.path);
            routes.add(toCoords(best.path, nodesMap));
        }

        return routes;
    }



}
