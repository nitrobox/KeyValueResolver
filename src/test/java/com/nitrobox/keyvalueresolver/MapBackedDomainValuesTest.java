package com.nitrobox.keyvalueresolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MapBackedDomainValuesTest {


    public static final List<String> DOMAIN_LIST = List.of("domain1", "domain2", "domain3");

    @Test
    void shouldMapAllValues() {
        // given
        MapBackedDomainValues domainValues = new MapBackedDomainValues();
        domainValues.set("domain1", "domain_value_1");
        domainValues.set("domain2", "domain_value_2");
        domainValues.set("domain3", "domain_value_3");

        // when
        String[] domainValuesAsStringArray = domainValues.getDomainValues(DOMAIN_LIST);
        // then
        String[] expectedArray = new String[]{"domain_value_1", "domain_value_2", "domain_value_3"};
        assertThat(domainValuesAsStringArray).isEqualTo(expectedArray);
    }

    @Test
    void shouldFillUpMissingValues() {
        // given
        MapBackedDomainValues domainValues = new MapBackedDomainValues();
        domainValues.set("domain1", "domain_value_1");
        domainValues.set("domain2", null);
        domainValues.set("domain3", "domain_value_3");

        // when
        String[] domainValuesAsStringArray = domainValues.getDomainValues(DOMAIN_LIST);
        // then
        String[] expectedArray = new String[]{"domain_value_1", null, "domain_value_3"};
        assertThat(domainValuesAsStringArray).isEqualTo(expectedArray);
    }
    
}