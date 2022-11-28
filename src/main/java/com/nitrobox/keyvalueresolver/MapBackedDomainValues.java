package com.nitrobox.keyvalueresolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;

public class MapBackedDomainValues implements DomainValues{


    private final Map<String, String> map = new HashMap<>();
    

    @Override
    public String[] getDomainValues(List<String> domains) {
        String[] domainValues = new String[domains.size()];
        IntStream.range(0,domains.size())
                .boxed()
                .map(index -> Pair.of(index, domains.get(index)))
                .map(pair -> Pair.of(pair.getKey(),map.get(pair.getValue())))
                .forEach(pair -> domainValues[pair.getKey()]= Optional.ofNullable(pair.getValue()).orElse("*"));
        return domainValues;
    }

    public MapBackedDomainValues set(final String domain, final String domainValue) {
        map.put(domain, domainValue);
        return this;
    }
}
