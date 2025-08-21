package org.example.shadowwalkerv2.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.shadowwalkerv2.dto.OverpassElement;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class OverpassResponse {
    private List<OverpassElement> elements;
}
