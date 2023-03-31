/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.query;

import org.eclipse.edc.spi.message.Range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuerySpecTest {

    @Test
    void verifyIllegalArguments() {
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().limit(-10).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().limit(0).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().offset(-10).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyDefaults() {
        var spec = QuerySpec.Builder.newInstance().build();
        var assertion = assertThat(spec);

        assertion.extracting(QuerySpec::getFilterExpression).isNotNull();
        assertion.extracting(QuerySpec::getLimit).isEqualTo(50);
        assertion.extracting(QuerySpec::getOffset).isEqualTo(0);
        assertion.extracting(QuerySpec::getSortOrder).isEqualTo(SortOrder.ASC);
        assertion.extracting(QuerySpec::getSortField).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "name=foo", "name = foo", "name =      foo", "name contains foo" })
    void verifyEquals_whenEqualsAsContainsFilterExpressions(String equalityExp) {
        var spec = QuerySpec.Builder.newInstance().equalsAsContains(true).filter(equalityExp).build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("name", "contains", "foo"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "name=foo", "name = foo", "name =      foo" })
    void verifyEquals_whenEqualsFilterExpressions(String equalityExp) {
        var spec = QuerySpec.Builder.newInstance().equalsAsContains(false).filter(equalityExp).build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("name", "=", "foo"));
    }

    @Test
    void verifyFilterExpression() {
        var spec = QuerySpec.Builder.newInstance().filter("age < 14").build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("age", "<", "14"));

        var spec2 = QuerySpec.Builder.newInstance().filter("age     <   14").build();
        assertThat(spec2.getFilterExpression()).hasSize(1).containsOnly(new Criterion("age", "<", "14"));

        var spec3 = QuerySpec.Builder.newInstance().filter("description endsWith 'foobar'").build();
        assertThat(spec3.getFilterExpression()).hasSize(1).containsOnly(new Criterion("description", "endsWith", "'foobar'"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "age<14", "namecontainssomething", "id_like_14" })
    void verify_invalidFilterExpression(String expr) {
        assertThatThrownBy(() -> QuerySpec.Builder.newInstance().filter(expr).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verify_filterExprNull() {
        List<Criterion> filter = null;
        var qs = QuerySpec.Builder.newInstance().filter(filter).build();

        assertThat(qs.getFilterExpression()).isNotNull().isEmpty();
    }

    @Test
    void verify_filterEqualsWithValueContainsEqualsSign() {
        var spec = QuerySpec.Builder.newInstance().equalsAsContains(true).filter("additionalProp1=a/b=c/d").build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("additionalProp1", "contains", "a/b=c/d"));

        var spec2 = QuerySpec.Builder.newInstance().equalsAsContains(false).filter("additionalProp1=a/b=c/d").build();
        assertThat(spec2.getFilterExpression()).hasSize(1).containsOnly(new Criterion("additionalProp1", "=", "a/b=c/d"));
    }

    @Test
    void verify_filterEqualsWithValueContainsSpaces() {
        var spec = QuerySpec.Builder.newInstance().equalsAsContains(true).filter("additionalProp1=a/b c/d").build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("additionalProp1", "contains", "a/b c/d"));

        var spec2 = QuerySpec.Builder.newInstance().equalsAsContains(false).filter("additionalProp1=a/b c/d").build();
        assertThat(spec2.getFilterExpression()).hasSize(1).containsOnly(new Criterion("additionalProp1", "=", "a/b c/d"));
    }

    @Test
    void verify_filterWithValueContainsSpaces() {
        var spec = QuerySpec.Builder.newInstance().filter("key instanceof some value").build();
        assertThat(spec.getFilterExpression()).hasSize(1).containsOnly(new Criterion("key", "instanceof", "some value"));
    }

    @Test
    void range_verifyCorrectConversion() {
        var spec = QuerySpec.Builder.newInstance()
                .range(new Range(37, 40))
                .build();

        assertThat(spec.getLimit()).isEqualTo(3);
        assertThat(spec.getOffset()).isEqualTo(37);

    }

    @Test
    void getRange_verifyCorrectConversion() {
        var spec = QuerySpec.Builder.newInstance()
                .limit(20)
                .offset(37)
                .build();

        var range = spec.getRange();
        assertThat(range.getFrom()).isEqualTo(37);
        assertThat(range.getTo()).isEqualTo(57);
    }

}