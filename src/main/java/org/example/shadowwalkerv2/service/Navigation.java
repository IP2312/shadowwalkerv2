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


//        // --- Suggested waypoints in Vienna 1st district (lat, lon) ---
//        GeoCoordinate GRABEN_E        = new GeoCoordinate(48.208900, 16.371700);
//        GeoCoordinate KOHLMARKT       = new GeoCoordinate(48.207600, 16.368900);
//        GeoCoordinate MICHAELERPLATZ  = new GeoCoordinate(48.206520, 16.365680); // same as typical 'goal'
//        GeoCoordinate KAERNTNER_STR   = new GeoCoordinate(48.205900, 16.372500);
//        GeoCoordinate ALBERTINA       = new GeoCoordinate(48.203600, 16.368900);
//        GeoCoordinate AUGUSTINERKIRCHE= new GeoCoordinate(48.205500, 16.370200);
//        GeoCoordinate PETERSKIRCHE    = new GeoCoordinate(48.208500, 16.372500);
//        GeoCoordinate AM_HOF          = new GeoCoordinate(48.210000, 16.369200);
//        GeoCoordinate MINORITENPLATZ  = new GeoCoordinate(48.209300, 16.366100);
//        GeoCoordinate TUCHLAUBEN      = new GeoCoordinate(48.210300, 16.371000);
//        GeoCoordinate FREYUNG         = new GeoCoordinate(48.212000, 16.366700);
//        GeoCoordinate CAFE_CENTRAL    = new GeoCoordinate(48.210600, 16.365800);
//        GeoCoordinate ROTENTURMSTR    = new GeoCoordinate(48.208900, 16.377900);
//        GeoCoordinate HOHER_MARKT     = new GeoCoordinate(48.212100, 16.373200);
//        GeoCoordinate JUDENPLATZ      = new GeoCoordinate(48.211100, 16.370000);
//        GeoCoordinate NEUER_MARKT     = new GeoCoordinate(48.205000, 16.372100);
//        GeoCoordinate ALBERTINAPLATZ  = new GeoCoordinate(48.203300, 16.368600);
//        GeoCoordinate BURGGARTEN      = new GeoCoordinate(48.203900, 16.366100);
//        GeoCoordinate HELDENPLATZ     = new GeoCoordinate(48.206900, 16.363400);
//        GeoCoordinate SINGERSTR       = new GeoCoordinate(48.207200, 16.375000);
//        GeoCoordinate FRANZISKANERPL  = new GeoCoordinate(48.206600, 16.372900);
//        GeoCoordinate AUGUSTINERSTR   = new GeoCoordinate(48.205800, 16.370900);
//
//        // Route 1: start → Graben → Kohlmarkt → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, GRABEN_E, KOHLMARKT, goal
//        )));
//
//        // Route 2: start → Kärntner Straße → Albertina → Augustinerkirche → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, KAERNTNER_STR, ALBERTINA, AUGUSTINERKIRCHE, goal
//        )));
//
//        // Route 3: start → Peterskirche → Am Hof → Minoritenplatz → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, PETERSKIRCHE, AM_HOF, MINORITENPLATZ, goal
//        )));
//
//        // Route 4: start → Tuchlauben → Freyung → Café Central → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, TUCHLAUBEN, FREYUNG, CAFE_CENTRAL, goal
//        )));
//
//        // Route 5: start → Rotenturmstraße → Hoher Markt → Judenplatz → Kohlmarkt → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, ROTENTURMSTR, HOHER_MARKT, JUDENPLATZ, KOHLMARKT, goal
//        )));
//
//        // Route 6: start → Neuer Markt → Albertinaplatz → Burggarten → Heldenplatz → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, NEUER_MARKT, ALBERTINAPLATZ, BURGGARTEN, HELDENPLATZ, goal
//        )));
//
//        // Route 7: start → Singerstraße → Franziskanerplatz → Augustinerstraße → goal
//        routes.add(new ArrayList<>(Arrays.asList(
//                start, SINGERSTR, FRANZISKANERPL, AUGUSTINERSTR, goal
//        )));
        System.out.println("no path found");
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
