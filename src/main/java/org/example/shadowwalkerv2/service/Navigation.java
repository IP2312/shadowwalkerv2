package org.example.shadowwalkerv2.service;


import org.example.shadowwalkerv2.dto.OverpassElement;
import org.example.shadowwalkerv2.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
public class Navigation {
    private final OverpassService overpassService;
    private final MapService mapService;
    private final Frontier frontier;

    public Navigation() {
        this.overpassService = new OverpassService();
        this.mapService = new MapService();
        this.frontier = new Frontier();

    }

    public ArrayList<GeoCoordinate> findeRoute(GeoCoordinate start, GeoCoordinate goal){
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
            nodesMap.put(n.getId(), n);
        }

        RouteNode startNode = getClosestNode(start, routeNodes);
        RouteNode goalNode = getClosestNode(goal, routeNodes);
        if (startNode == null || goalNode == null) {
            //TODO ecxeption
            return routeCoordinates;
        }

        //Path finding A*
        frontier.clear();

        RouteNode currentNode = startNode;
        currentNode.setExplored(true);
        currentNode.setCostToReachNode(0);

        int count = 0;
        while (!currentNode.equals(goalNode)) {
            ArrayList<RoutWay> possibleRouts = getRoutsFromNode(currentNode, routs);
            ArrayList<Long> neighbourIds = findNeighboursId(currentNode, possibleRouts);

            for (Long neighbourId : neighbourIds) {
                RouteNode neighbour = nodesMap.get(neighbourId);
                if (!neighbour.isExplored()){
                    calculateCost(currentNode, neighbour, goalNode);
                    frontier.addNode(neighbour);
                }
            }
            currentNode.setChildNode(frontier.removeNode());
            currentNode.getChildNode().setParentNode(currentNode);
            currentNode = currentNode.getChildNode();

            System.out.println(count++);
        }
        System.out.println(currentNode);


        System.out.println("coordinates");
        routeCoordinates.add(start);
        routeCoordinates.add(new GeoCoordinate(48.310661362874455, 14.291979911708491));
        routeCoordinates.add(new GeoCoordinate(48.3111889542115, 14.29258036938026));
        routeCoordinates.add(goal);


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

    public ArrayList<Long> findNeighboursId(RouteNode currentNode, ArrayList<RoutWay> possibleRouts) {
        ArrayList<Long> neighboursId = new ArrayList<>();

        for (RoutWay rout : possibleRouts) {
            for (int i = 0; i < rout.getNodesId().size(); i++) {
                if (rout.getNodesId().get(i) == currentNode.getId()){
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

    public void calculateCost(RouteNode currentNode, RouteNode neighbour, RouteNode goal){

        double coveredDistance = currentNode.getCostToReachNode();
        coveredDistance += mapService.haversineDistance(currentNode.getCoordinate(), neighbour.getCoordinate());
        neighbour.setCostToReachNode(coveredDistance);
        neighbour.setEstimatedCostToGoal(mapService.haversineDistance(neighbour.getCoordinate(), goal.getCoordinate()));

    }

}
