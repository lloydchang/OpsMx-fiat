/*
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.fiat;

import com.netflix.spinnaker.config.ErrorConfiguration;
import com.netflix.spinnaker.kork.boot.DefaultPropertiesBuilder;
import java.util.Map;
import org.springframework.boot.actuate.autoconfigure.ldap.LdapHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
@ComponentScan({
  "com.netflix.spinnaker.fiat",
  "com.netflix.spinnaker.config",
})
@Import(ErrorConfiguration.class)
@EnableAutoConfiguration(
    exclude = {
      GsonAutoConfiguration.class,
      // Disable LDAP health check until we pull in the fix to
      // https://github.com/spring-projects/spring-ldap/issues/473
      LdapHealthContributorAutoConfiguration.class,
      // SQL is optional
      DataSourceAutoConfiguration.class,
    })
public class Main extends SpringBootServletInitializer {

  private static final Map<String, Object> DEFAULT_PROPS =
      new DefaultPropertiesBuilder().property("spring.application.name", "fiat").build();

  public static void main(String... args) {
    new SpringApplicationBuilder().properties(DEFAULT_PROPS).sources(Main.class).run(args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.properties(DEFAULT_PROPS).sources(Main.class);
  }
}
