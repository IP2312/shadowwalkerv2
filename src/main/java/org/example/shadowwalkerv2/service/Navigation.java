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

}
