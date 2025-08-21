package org.example.shadowwalkerv2.model;

import org.example.shadowwalkerv2.model.enums.Objects;
import org.example.shadowwalkerv2.model.imp.Way;

import java.util.ArrayList;
import java.util.Arrays;

public class RoutWay implements Way {
    private long id;
    private String type;
    private ArrayList<Long> nodesId;

    public RoutWay(long id, String type, ArrayList<Long> nodesId) {
        setId(id);
        setType(type);
        setNodesId(nodesId);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        try {
            this.id = Long.parseLong(String.valueOf(id));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            System.out.println("Id is not a number");
        }
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String input) {
        if (Arrays.stream(Objects.values())
                .anyMatch(e -> e.name().equalsIgnoreCase(input))) {
            this.type = input.toLowerCase();
        } else {
            throw new IllegalArgumentException("Type is not valid");


        }

    }

    @Override
    public ArrayList<Long> getNodesId() {
        return nodesId;
    }

    @Override
    public void setNodesId(ArrayList<Long> nodesId) {
        this.nodesId = nodesId;
    }

    @Override
    public String toString() {
        return "Way{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", nodes=" + nodesId +
                '}';
    }
}
