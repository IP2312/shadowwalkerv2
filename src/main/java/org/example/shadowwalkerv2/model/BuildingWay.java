package org.example.shadowwalkerv2.model;

import lombok.Getter;
import org.example.shadowwalkerv2.model.imp.Way;

import java.util.ArrayList;


public class BuildingWay implements Way {
    private long id;
    private String type;
    private ArrayList<Long> nodesId;
    @Getter
    private int height;
    @Getter
    private int levels;

    public BuildingWay(long id, String type, ArrayList<Long> nodesId) {
        this.id = id;
        this.type = type;
        this.nodesId = nodesId;
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
        this.type = input.trim();
    }

    @Override
    public ArrayList<Long> getNodesId() {
        return nodesId;
    }

    @Override
    public void setNodesId(ArrayList<Long> nodesId) {
        this.nodesId = nodesId;
    }

    public void setHeight(String height) {
        try{
            this.height = Integer.parseInt(String.valueOf(height));
        }catch (NumberFormatException e){
            this.height = 0;
            System.out.println("Height is not a number");
        }
    }

    public void setLevels(String levels) {
        try{
            this.levels = Integer.parseInt(String.valueOf(levels));
        }catch (NumberFormatException e){

            System.out.println("Levels is not a number");
        }
    }

    @Override
    public String toString() {
        return "BuildingWay{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", nodesId=" + nodesId +
                ", height=" + height +
                ", levels=" + levels +
                '}';
    }
}
