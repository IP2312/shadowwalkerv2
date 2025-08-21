package org.example.shadowwalkerv2.model.imp;

import java.util.ArrayList;

public interface Way {
    long getId();

    void setId(long id);

    String getType();

    void setType(String input);

    ArrayList<Long> getNodesId();

    void setNodesId(ArrayList<Long> nodesId);

    @Override
    String toString();
}
