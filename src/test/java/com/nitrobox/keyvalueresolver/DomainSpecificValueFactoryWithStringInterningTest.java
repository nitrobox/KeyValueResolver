package com.nitrobox.keyvalueresolver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Created by Benjamin Jochheim on 10.11.15.
 */
public class DomainSpecificValueFactoryWithStringInterningTest {

    private final DomainSpecificValueFactory factory = new DomainSpecificValueFactoryWithStringInterning();

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
    void testInterningOfStrings() {

        final String value1 = new String("testString");
        final String value2 = new String("testString");
        assertThat(value1).isNotSameAs(value2);
        assertThat(value1).isEqualTo(value2);

        DomainSpecificValue dsv1 = factory.create(value1, null, "DE", "de_DE");
        DomainSpecificValue dsv2 = factory.create(value2, null, "DE", "de_DE");

        assertThat(dsv1.getValue()).isSameAs(dsv2.getValue());
        assertThat((String) dsv1.getValue()).isEqualTo("testString");
    }
}
