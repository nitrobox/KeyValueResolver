package com.nitrobox.keyvalueresolver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KeyValueResolverGetAllKeyValuesTest {

    private DomainResolver resolver;
    private KeyValueResolver keyValueResolver;

    @BeforeEach
    void before() {
        resolver = new MapBackedDomainResolver()
                .set("dom1", "val1")
                .set("dom2", "val2")
                .set("dom3", "val3")
                .set("dom4", "val4");
        keyValueResolver = new KeyValueResolverImpl("dom1", "dom2", "dom3", "dom4");
    }

    @Test
    void emptyKeyValueResolverGivesEmptyKeyValues() {
        assertThat(keyValueResolver.getAllKeyValues()).isEmpty();
        assertThat(keyValueResolver.getAllKeyValues(resolver)).isEmpty();
    }

    @Test
    void getAllKeyValuesGivesEverythingUnfiltered() {
        keyValueResolver.set("key1", "value_1", "desc");
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1");
        keyValueResolver.set("key1", "value_dom2", "desc", "domval2");
        Collection<KeyValues> allKeyValues = keyValueResolver.getAllKeyValues();
        assertThat(allKeyValues).hasSize(1);
        final KeyValues keyValues = allKeyValues.iterator().next();
        assertThat(keyValues.getKey()).isEqualTo("key1");
        final Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("value_1", null),
                new DefaultDomainSpecificValueFactory().create("value_dom1", null, "domval1"),
                new DefaultDomainSpecificValueFactory().create("value_dom2", null, "domval2")
        );
    }

    @Test
    void getAllKeyValuesWithResolverGivesFilteredResult() {
        keyValueResolver.set("key1", "value_1", "desc");
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1");
        keyValueResolver.set("key1", "value_dom_2", "desc", "domval1", "domval2");
        keyValueResolver.set("key1", "value_dom_*", "desc", "*", "domval_other_2");
        keyValueResolver.set("key1", "value_dom_another", "desc", "domval_another");
        keyValueResolver.set("key1", "value_dom_other", "desc", "domval_other", "domval2");
        keyValueResolver.set("key2", "val2", "desc", "other");

        Collection<KeyValues> allKeyValues = keyValueResolver.getAllKeyValues(new MapBackedDomainResolver().set("dom1", "domval1"));
        assertThat(allKeyValues).hasSize(1);

        final KeyValues keyValues = allKeyValues.iterator().next();
        assertThat(keyValues.getKey()).isEqualTo("key1");
        final Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("value_dom1", null, "domval1"),
                new DefaultDomainSpecificValueFactory().create("value_dom_2", null, "domval1", "domval2")
        );
    }

    @Test
    void getAllKeyValuesWithResolverWithMultipleKeysGivesFilteredResult() {
        keyValueResolver.set("key1", "value_1", "desc");
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1");
        keyValueResolver.set("key1", "value_dom_other", "desc", "domval_other");

        keyValueResolver.set("key2", "key2_dom1", "desc", "domval1");
        keyValueResolver.set("key2", "key2_dom_other", "desc", "domval_other");

        Collection<KeyValues> allKeyValues = keyValueResolver.getAllKeyValues("domval1");
        assertThat(allKeyValues).hasSize(2);

        final KeyValues keyValues = allKeyValues.stream().filter(kv -> kv.getKey().equals("key1")).findFirst().orElseThrow();
        assertThat(keyValues.getKey()).isEqualTo("key1");
        final Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("value_dom1", null, "domval1")
        );

        final KeyValues keyValues2 = allKeyValues.stream().filter(kv -> kv.getKey().equals("key2")).findFirst().orElseThrow();
        assertThat(keyValues2.getKey()).isEqualTo("key2");
        final Set<DomainSpecificValue> domainSpecificValues2 = keyValues2.getDomainSpecificValues();
        assertThat(domainSpecificValues2).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("key2_dom1", null, "domval1")
        );
    }

    @Test
    void keysInAChangeSetAreNotReturnedWhenTheChangeSetIsNotActive() {
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1");
        keyValueResolver.setWithChangeSet("key1", "CS_value", "desc", "ChangeSet", "domval1");

        Collection<KeyValues> allKeyValues = keyValueResolver.getAllKeyValues(new MapBackedDomainResolver().set("dom1", "domval1"));
        assertThat(allKeyValues).hasSize(1);

        final KeyValues keyValues = allKeyValues.iterator().next();
        assertThat(keyValues.getKey()).isEqualTo("key1");
        final Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("value_dom1", null, "domval1")
        );
    }

    @Test
    void theHighestPrecedenceKeysInAChangeSetAreReturnedWhenTheChangeSetIsActive() {
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1");
        keyValueResolver.set("key1", "val", "desc", "domval1", "domval2");
        keyValueResolver.setWithChangeSet("key1", "CS_value", "desc", "ChangeSet", "domval1");
        keyValueResolver.setWithChangeSet("key1", "ACS_value", "desc", "AChangeSet", "domval1");

        Collection<KeyValues> allKeyValues = keyValueResolver.getAllKeyValues(new MapBackedDomainResolver()
                .set("dom1", "domval1")
                .addActiveChangeSets("ChangeSet", "AChangeSet"));
        assertThat(allKeyValues).hasSize(1);

        final KeyValues keyValues = allKeyValues.iterator().next();
        assertThat(keyValues.getKey()).isEqualTo("key1");
        final Set<DomainSpecificValue> domainSpecificValues = keyValues.getDomainSpecificValues();
        assertThat(domainSpecificValues).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("val", null, "domval1", "domval2"),
                new DefaultDomainSpecificValueFactory().create("ACS_value", "AChangeSet", "domval1")
        );
    }

    @Test
    void getKeyValuesForASpecificDomainResolverGivesOnlyValuesForMatchingDomains() {
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1");
        keyValueResolver.set("key1", "val", "desc", "domvalOther", "domval2");
        keyValueResolver.set("key1", "value_dom2", "desc", "domval1", "domval2");
        final KeyValues keyValues = keyValueResolver.getKeyValues("key1", new MapBackedDomainResolver()
                .set("dom1", "domval1"));
        assertThat(keyValues.getDomainSpecificValues()).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("value_dom1", null, "domval1"),
                new DefaultDomainSpecificValueFactory().create("value_dom2", null, "domval1", "domval2")
        );
    }
    
    @Test
    void onlyTheBestMatchingKeyValuesAreReturned() {
        keyValueResolver.set("key1", "value_dom2", "desc", "*", "domval2");
        keyValueResolver.set("key1", "value_dom1&2", "desc", "domval1", "domval2");
        keyValueResolver.set("key1", "value_dom1", "desc", "domval1", "*");

        Collection<KeyValues> allKeyValues = keyValueResolver.getAllKeyValues(new MapBackedDomainResolver()
                .set("dom1", "domval1")
                .set("dom2", "domval2"));
        assertThat(allKeyValues).hasSize(1);
        final KeyValues keyValues = allKeyValues.iterator().next();
        assertThat(keyValues.getDomainSpecificValues()).containsExactlyInAnyOrder(
                new DefaultDomainSpecificValueFactory().create("value_dom1&2", null, "domval1", "domval2")
        );
    }
}
