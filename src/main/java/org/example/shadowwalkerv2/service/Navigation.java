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
        //todo get time from frontend and set Targets
        double target = 30;
        ZonedDateTime time = ZonedDateTime.now();
        ArrayList<GeoCoordinate> routeCoordinates = new ArrayList<>();

        OverpassResponse routElements = overpassService.loadRouts(start, goal);
        ArrayList<RouteNode> routeNodes = new ArrayList<>();
        ArrayList<RoutWay> routs = new ArrayList<>();

        for (OverpassElement element : routElements.getElements()) {
            if (element.type.equals("way")) {
                routs.add(new RoutWay(element.id, "road", (ArrayList<Long>) element.nodes));
            } else if (element.type.equals("node")) {
                routeNodes.add(new RouteNode(element.id, new GeoCoordinate(element.lat, element.lon)));
            }
        }

        OverpassResponse buildingElements = overpassService.loadBuildings(start, goal);
        LinkedHashSet<BuildingNode> buildingNodes = new LinkedHashSet<>();
        ArrayList<BuildingWay> buildings = new ArrayList<>();

        for (OverpassElement element : buildingElements.getElements()) {
            if (element.type.equals("way")) {
                BuildingWay newBuilding = new BuildingWay(element.id, "building", (ArrayList<Long>) element.nodes);
                newBuilding.setHeight(element.tags.get("height"));
                newBuilding.setLevels(element.tags.get("building:levels"));
                buildings.add(newBuilding);
            } else if (element.type.equals("node")) {
                buildingNodes.add(new BuildingNode(element.id, new GeoCoordinate(element.lat, element.lon)));
            }
        }


        if (routeNodes.isEmpty()) {
            //TODO throw exception no nodes returned by Overpass—return empty
            return routeCoordinates;
        }

        HashMap<Long, RouteNode> nodesMap = new HashMap<>();
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
            //TODO exception
            return routeCoordinates;
        }

        //Path finding A*
        frontier.clear();

        Map<Long, Boolean> shadedCache = new HashMap<>(routeNodes.size() * 2);
        Function<RouteNode, Boolean> isShaded = rn ->
                shadedCache.computeIfAbsent(
                        rn.getId(),
                        id -> sunService.checkForShade(rn, buildings, buildingNodes, time)
                );

        //record Key (double g, double shade){}
        //HashMap<Long,Key> bestKey = new HashMap<>();

        startNode.setCostToReachNode(0);
        startNode.setEstimatedCostToGoal(mapService.haversineDistance(startNode.getCoordinate(), goalNode.getCoordinate()));
        startNode.setTotalCount(1);
        startNode.setShadedCount(isShaded.apply(startNode) ? 1 : 0);
        //bestKey.put(startNode.getId(), new Key(startNode.getCostToReachNode(),startNode.shadeRatio()));
        frontier.addOrUpdateNode(startNode);
        //RouteNode bestGoal = null;


        while (!frontier.isEmpty()) {
            RouteNode currentNode = frontier.removeNode();
            //Todo exception
            if (currentNode == null) break;
            if (currentNode.equals(goalNode)) {
                ArrayList<RouteNode> path = reconstructPath(currentNode);

                double nodesInSun = 0;
                double nodesInShade = 0;
                for (RouteNode rn : path) {
                    if (sunService.checkForShade(rn,buildings,buildingNodes, time)){
                        nodesInShade++;
                    }
                    else {
                        nodesInSun++;
                    }
                    routeCoordinates.add(rn.getCoordinate());
                }
                double shadow = nodesInShade/(nodesInShade + nodesInSun) * 100;
                System.out.println("Shadow:" + shadow + "%");

                if (shadow > target){
                    return routeCoordinates;
                }
                System.out.println("above target");
                currentNode = frontier.removeNode();

            }
            ArrayList<RoutWay> possibleRouts = getRoutsFromNode(currentNode, routs);
            LinkedHashSet<Long> neighbourIds = findNeighboursId(currentNode, possibleRouts);

            for (Long neighbourId : neighbourIds) {
                RouteNode neighbour = nodesMap.get(neighbourId);
                //todo null
                if (currentNode.getParentNode() == null || !currentNode.getParentNode().equals(neighbour)) {
                    //calculateCost(currentNode, neighbour, goalNode);
                    double distanceToNeighbour = mapService.haversineDistance(currentNode.getCoordinate(), neighbour.getCoordinate());
                    double tentativeG = currentNode.getCostToReachNode() + distanceToNeighbour;

                    //prevent choosing a worse path if neighbour not explored -> POSITIVE_INFINITY or previous explored path worse
                    if (tentativeG < neighbour.getCostToReachNode()) {
                        neighbour.setParentNode(currentNode);
                        neighbour.setCostToReachNode(tentativeG);
                        neighbour.setEstimatedCostToGoal(mapService.haversineDistance(neighbour.getCoordinate(), goalNode.getCoordinate())
                        );
                        frontier.addOrUpdateNode(neighbour);
                    }
                }
            }

        }
        //todo exception
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
                if (nodeId == node.getId()) {
                    newRouts.add(rout);
                }
            }
        }
        return newRouts;
    }

    public LinkedHashSet<Long> findNeighboursId(RouteNode currentNode, ArrayList<RoutWay> possibleRouts) {
        LinkedHashSet<Long> neighboursId = new LinkedHashSet<>();

        for (RoutWay rout : possibleRouts) {
            for (int i = 0; i < rout.getNodesId().size(); i++) {
                if (rout.getNodesId().get(i) == currentNode.getId()) {
                    if (i > 0) {
                        neighboursId.add(rout.getNodesId().get(i - 1));
                    }
                    if (i < rout.getNodesId().size() - 1) {
                        neighboursId.add(rout.getNodesId().get(i + 1));
                    }
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
