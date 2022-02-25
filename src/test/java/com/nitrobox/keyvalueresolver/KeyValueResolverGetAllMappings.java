package com.nitrobox.keyvalueresolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class KeyValueResolverGetAllMappings {

    private final KeyValueResolverImpl keyValueResolver = new KeyValueResolverImpl();
    private final MapBackedDomainResolver resolver = new MapBackedDomainResolver();

    @Test
    void getAnEmptyMapFromAnEmptyKeyValueResolver() {
        assertThat(keyValueResolver.getAllMappings(resolver)).isEmpty();
    }

    @Test
    void keyValueResolverWithOneDefaultValueIsReturnedInMapping() {
        keyValueResolver.set("key", "value", "desc");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1);
        assertThat(keyValueResolver.getAllMappings(resolver)).containsAllEntriesOf(Map.of("key", "value"));
    }

    @Test
    void onlyTheBestMatchingValueIsUsed() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value", "desc");
        keyValueResolver.set("key", "value1", "desc", "*", "dom2");
        keyValueResolver.set("key", "value2", "desc", "dom1", "dom2");
        resolver.set("domain1", "dom1").set("domain2", "dom2");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1);
        assertThat(keyValueResolver.getAllMappings(resolver)).containsAllEntriesOf(Map.of("key", "value2"));
    }

    @Test
    void partialMappingsAreReturned() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value1", "desc", "*", "dom2");
        keyValueResolver.set("key", "value2", "desc", "dom1", "dom2");
        keyValueResolver.set("key", "value3", "desc", "dom1", "*");
        keyValueResolver.set("key2", "otherValue", "desc");
        assertThat(keyValueResolver.getAllMappings("dom1")).hasSize(2);
        assertThat(keyValueResolver.getAllMappings("dom1")).containsAllEntriesOf(Map.of("key", "value3", "key2", "otherValue"));
    }

    @Test
    void keysWithoutAMatchingDomainValueAreNotReturned() {
        keyValueResolver.addDomains("domain1", "domain2");
        keyValueResolver.set("key", "value1", "desc", "*", "dom2");
        keyValueResolver.set("key", "value2", "desc", "dom1", "dom2");
        keyValueResolver.set("key2", "otherValue", "desc");
        resolver.set("domain1", "dom1");
        assertThat(keyValueResolver.getAllMappings(resolver)).hasSize(1);
        assertThat(keyValueResolver.getAllMappings(resolver)).containsAllEntriesOf(Map.of("key2", "otherValue"));
    }
}
