/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.fiat.model.resources

import com.fasterxml.jackson.core.PrettyPrinter
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.netflix.spinnaker.fiat.YamlFileApplicationContextInitializer
import com.netflix.spinnaker.fiat.model.Authorization
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = TestConfig, initializers = YamlFileApplicationContextInitializer)
class PermissionsSpec extends Specification {

  private static final Authorization R = Authorization.READ
  private static final Authorization W = Authorization.WRITE
  private static final Authorization E = Authorization.EXECUTE
  private static final Authorization C = Authorization.CREATE

  @Autowired
  TestConfigProps testConfigProps

  // Make line endings consistent regardless of OS
  PrettyPrinter printer =
          new DefaultPrettyPrinter()
                  .withObjectIndenter(new DefaultIndenter().withLinefeed("\n"))

  ObjectMapper mapper =
          new ObjectMapper()
                  .enable(SerializationFeature.INDENT_OUTPUT)
                  .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)

  String permissionJson = '''\
    {
      "READ" : [ "foo" ],
      "WRITE" : [ "bar" ]
    }'''.stripIndent()

  String permissionSerialized = '''\
    {
      "READ" : [ "foo" ],
      "WRITE" : [ "bar" ]
    }'''.stripIndent()

  def "should deserialize"() {
    when:
    Permissions p = mapper.readValue(permissionJson, Permissions)

    then:
    p.get(R) == ["foo"] as Set
    p.get(W) == ["bar"] as Set
    p.get(E) == [] as Set

    when:
    Permissions.Builder b = mapper.readValue(permissionJson, Permissions.Builder)
    p = b.build()

    then:
    p.get(R) == ["foo"] as Set
    p.get(W) == ["bar"] as Set
    p.get(E) == [] as Set
  }

  def "should serialize"() {
    when:
    Permissions.Builder b = new Permissions.Builder()
    b.set([(R): ["foo"] as Set, (W): ["bar"] as Set])

    then:
    permissionSerialized ==  mapper.writer(printer).writeValueAsString(b.build())
  }

  def "can deserialize to builder from serialized Permissions"() {
    setup:
    Permissions.Builder b1 = new Permissions.Builder().add(W, "batman").add(R, "robin")
    Permissions p1 = b1.build()

    when:
    def serialized = mapper.writeValueAsString(p1)
    Permissions.Builder b2 = mapper.readValue(serialized, Permissions.Builder)
    Permissions p2 = b2.build()

    then:
    p1 == p2
  }

  def "should trim and lower"() {
    when:
    Permissions.Builder b = new Permissions.Builder()
    b.set([(R): ["FOO"] as Set])

    then:
    b.build().get(R) == ["foo"] as Set

    when:
    b.add(W, "bAr          ")

    then:
    b.build().get(W) == ["bar"] as Set
  }

  def "test immutability"() {
    setup:
    Permissions.Builder b = new Permissions.Builder().add(R, "foo").add(W, "bar")

    when:
    b.add(R, "baz")

    then:
    b.get(R).size() == 2

    when:
    Permissions im = b.build()
    im.get(R).clear()

    then:
    thrown UnsupportedOperationException
  }

  def "test allGroups"() {
    setup:
    Permissions.Builder b = new Permissions.Builder().add(R, "foo")

    expect:
    b.build().allGroups() == ["foo"] as Set

    when:
    Permissions p = Permissions.factory([(R): ["bar"] as Set, (W): ["bar"] as Set])

    then:
    p.allGroups() == ["bar"] as Set

    when:
    p = Permissions.factory([(R): ["foo"] as Set, (W): ["bar"] as Set])

    then:
    p.allGroups() == ["foo", "bar"] as Set

  }

  def "test isRestricted"() {
    expect:
    !(new Permissions.Builder().build().isRestricted())
    (new Permissions.Builder().add(R, "foo").build().isRestricted())
  }

  def "test getAuthorizations"() {
    setup:
    Permissions p = new Permissions.Builder().build()

    expect:
    p.getAuthorizations([]) == [R, W, E, C] as Set

    when:
    p = Permissions.factory([(R): ["bar"] as Set, (W): ["bar"] as Set])

    then:
    p.getAuthorizations(["bar"]) == [R, W] as Set

    when:
    p = Permissions.factory([(R): ["bar"] as Set])

    then:
    p.getAuthorizations(["bar", "foo"]) == [R] as Set
  }

  def "test config props deserialization"() {
    expect: "Parsed from test/resources/config/application.yml"
    testConfigProps != null
    testConfigProps.permissions != null

    when:
    Permissions p = testConfigProps.permissions.build()

    then:
    p.get(R) == ["foo"] as Set
    p.get(W) == ["bar"] as Set
  }

  @Configuration
  @EnableConfigurationProperties(TestConfigProps)
  static class TestConfig {
  }

  @ConfigurationProperties("test-root")
  static class TestConfigProps {
    Permissions.Builder permissions
  }
}
