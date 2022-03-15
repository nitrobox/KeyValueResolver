package com.nitrobox.keyvalueresolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * User: mjaeckel Date: 15.11.13 Time: 10:47
 */
public class DefaultDomainSpecificValueFactoryTest {

    private final DomainSpecificValueFactory factory = new DefaultDomainSpecificValueFactory();

    @Test
    void factoryCreatesCorrectDSVForBaseKey() {
        String value = "value";
        DomainSpecificValue dsv = factory.create(value, null);
        assertThat((String) dsv.getValue()).isEqualTo(value);
        assertThat(dsv.getPattern()).isEqualTo("");
    }

    @Test
    void factoryCreatesCorrectDSVForOverriddenKey() {
        String value = "overriddenValue";

        DomainSpecificValue dsv = factory.create(value, null, "DE", "de_DE");

        assertThat((String) dsv.getValue()).isEqualTo(value);
        assertThat(dsv.getPattern()).isEqualTo("DE|de_DE|");
    }

    @Test
    void factorySetReverseOrderForMoreSpecificValues() {
        String value = "overriddenValue";

        DomainSpecificValue dsv = factory.create(value, null, "DE");
        DomainSpecificValue moreSpecificDsv = factory.create(value, null, "DE", "de_DE");

        assertThat(dsv.compareTo(moreSpecificDsv)).isGreaterThan(0);
    }
    
    @Test
    void valueCreatedFromPatternIsTheSameAsFromDomainValues() {
        final DomainSpecificValue value = factory.createFromPattern("value", null, "DE|de|*|XXX|");
        final DomainSpecificValue valueCompare = factory.create("value", null, "DE", "de", "*", "XXX");
        assertThat(value).isEqualTo(valueCompare);
    }
    
    @Test
    void patternMustEndWithPipe() {
        assertThrows(IllegalArgumentException.class, () -> factory.createFromPattern("value", null, "asdf"));
    }
}
