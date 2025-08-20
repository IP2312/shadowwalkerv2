package org.example.shadowwalkerv2.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Component
public class OverpassElement {
    public String type;
    public long id;
    public List<Long> nodes;
    public Map<String, String> tags;
    public Double lat;
    public Double lon;
}
