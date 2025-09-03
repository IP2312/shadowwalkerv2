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

    public ArrayList<GeoCoordinate> findeRoute(GeoCoordinate start, GeoCoordinate goal) {
        System.out.println("Start:");
        ZonedDateTime time = ZonedDateTime.now().minusHours(1);

        final double targetPct = 70;   // percent
        final double maxStretch = 2;   // allow up to 80% longer than shortest
        final double minShadeGain = 0.1; // require +10% better shade to accept worse g
        final double EPS = 1e-9;

        ArrayList<GeoCoordinate> routeCoordinates = new ArrayList<>();

        // ---- Build graph (SAFE copies) ----
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

        if (routeNodes.isEmpty()) return routeCoordinates;

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
        RouteNode goalNode  = getClosestNode(goal, routeNodes);
        if (startNode == null || goalNode == null) return routeCoordinates;

        // Shade cache for this run
        Map<Long, Boolean> shadedCache = new HashMap<>(routeNodes.size() * 2);
        Function<RouteNode, Boolean> isShaded = rn ->
                shadedCache.computeIfAbsent(
                        rn.getId(),
                        id -> sunService.checkForShade(rn, buildings, buildingNodes, time)
                );

        // ---- Init frontier ----
        frontier.clear();
        startNode.setCostToReachNode(0.0);
        startNode.setEstimatedCostToGoal(mapService.haversineDistance(startNode.getCoordinate(), goalNode.getCoordinate()));
        startNode.setTotalCount(1);
        startNode.setShadedCount(isShaded.apply(startNode) ? 1 : 0);
        frontier.addOrUpdateNode(startNode);

        double shortestDist = Double.NaN;
        Double shortestShadePct = null;
        ArrayList<RouteNode> shortestPathNodes = null;

        while (!frontier.isEmpty()) {
            RouteNode currentNode = frontier.removeNode();
            if (currentNode == null) break;

            // current explored rout to long skip node
            if (!Double.isNaN(shortestDist) && maxStretch < Double.POSITIVE_INFINITY) {
                if (currentNode.getCostToReachNode() > shortestDist * maxStretch) continue;
            }

            // Goal handling
            if (currentNode.equals(goalNode)) {
                ArrayList<RouteNode> path = reconstructPath(currentNode);

                int shaded = 0;
                for (RouteNode rn : path) if (isShaded.apply(rn)) shaded++;
                double shadePct = (path.isEmpty() ? 0.0 : 100.0 * shaded / path.size());
                System.out.println("Candidate goal shade: " + shadePct + "%");

                if (Double.isNaN(shortestDist)) {
                    shortestPathNodes = path;
                    shortestDist = currentNode.getCostToReachNode(); // g at goal
                    shortestShadePct = shadePct;                     // record shade of shortest
                    System.out.printf("Shortest: %.0fm, shade %.1f%%%n", shortestDist, shortestShadePct);
                }

                if (shadePct + EPS >= targetPct) {
                    // Return ONLY when we accept; do not add coords elsewhere
                    for (RouteNode rn : path) routeCoordinates.add(rn.getCoordinate());
                    System.out.println("success");
                    return routeCoordinates;
                }
                // below target -> keep searching
                continue;
            }

            // Expand neighbors
            ArrayList<RoutWay> possibleRouts = getRoutsFromNode(currentNode, routs);
            LinkedHashSet<Long> neighbourIds = findNeighboursId(currentNode, possibleRouts);

            for (Long neighbourId : neighbourIds) {
                RouteNode neighbour = nodesMap.get(neighbourId);
                if (neighbour == null) continue;

                // prevent immediate backtrack (FIX: && instead of ||)
                if (currentNode.getParentNode() != null && currentNode.getParentNode().equals(neighbour)) continue;

                double edge = mapService.haversineDistance(currentNode.getCoordinate(), neighbour.getCoordinate());
                double tentativeG = currentNode.getCostToReachNode() + edge;

                // carry shade stats
                int nextTotal  = currentNode.getTotalCount() + 1;
                int nextShaded = currentNode.getShadedCount() + (isShaded.apply(neighbour) ? 1 : 0);
                double nextShadeRatio = (nextTotal == 0) ? 0.0 : (double) nextShaded / nextTotal;

                double oldG     = neighbour.getCostToReachNode();
                double oldShade = neighbour.shadeRatio();

                boolean betterG  = tentativeG + EPS < oldG;
                boolean equalG   = Math.abs(tentativeG - oldG) <= EPS;
                boolean betterShadeAtSameG = equalG && nextShadeRatio > oldShade + EPS;
                boolean worseGBetterShade  = !betterG && !equalG && nextShadeRatio >= oldShade + minShadeGain;

                if (betterG || betterShadeAtSameG || worseGBetterShade || oldG == Double.POSITIVE_INFINITY) {
                    neighbour.setParentNode(currentNode);
                    neighbour.setCostToReachNode(tentativeG);
                    neighbour.setEstimatedCostToGoal(mapService.haversineDistance(neighbour.getCoordinate(), goalNode.getCoordinate()));
                    neighbour.setTotalCount(nextTotal);
                    neighbour.setShadedCount(nextShaded);
                    frontier.addOrUpdateNode(neighbour);
                }
            }
        }

        // ---- Fallback: return shortest if no shaded path reaches target ----
        if (shortestPathNodes != null) {
            for (RouteNode rn : shortestPathNodes) routeCoordinates.add(rn.getCoordinate());
            System.out.printf("Returning shortest (no shaded path ≥ %.0f%%). Shade: %.1f%%%n",
                    targetPct, shortestShadePct == null ? 0.0 : shortestShadePct);
            return routeCoordinates;
        }

        System.out.println("no Path found");
        return routeCoordinates;
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


    public ArrayList<RoutWay> getRoutsFromNode(RouteNode node, ArrayList<RoutWay> routs) {
        ArrayList<RoutWay> newRouts = new ArrayList<>();
        for (RoutWay rout : routs) {
            for (Long nodeId : rout.getNodesId()) {
                if (nodeId.equals(node.getId())) { // FIX
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

    public void calculateCost(RouteNode currentNode, RouteNode neighbour, RouteNode goal) {

        double coveredDistance = currentNode.getCostToReachNode();
        coveredDistance += mapService.haversineDistance(currentNode.getCoordinate(), neighbour.getCoordinate());
        neighbour.setCostToReachNode(coveredDistance);
        neighbour.setEstimatedCostToGoal(mapService.haversineDistance(neighbour.getCoordinate(), goal.getCoordinate()));

    }


    private ArrayList<RouteNode> reconstructPath(RouteNode goal) {
        ArrayList<RouteNode> path = new ArrayList<>();
        for (RouteNode n = goal; n != null; n = n.getParentNode()) {
            path.add(n); // prepend
        }
        Collections.reverse(path);
        return path;
    }

}
