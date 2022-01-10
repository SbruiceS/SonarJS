/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2012-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.javascript.it.plugin;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarsource.analyzer.commons.ProfileGenerator;
import org.sonarsource.analyzer.commons.ProfileGenerator.RulesConfiguration;

import static com.sonar.javascript.it.plugin.OrchestratorStarter.newWsClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@ExtendWith(OrchestratorStarter.class)
class CssIssuesTest {

  private static final String PROJECT_KEY = "css-issues-project";

  private static final Orchestrator orchestrator = OrchestratorStarter.ORCHESTRATOR;

  private static BuildResult buildResult;

  @BeforeAll
  public static void prepare() {
    RulesConfiguration rulesConfiguration = new RulesConfiguration();
    rulesConfiguration.add("S4660", "ignorePseudoElements", "ng-deep, /^custom-/");
    File profile = ProfileGenerator.generateProfile(orchestrator.getServer().getUrl(), "css", "css", rulesConfiguration, Collections.emptySet());
    orchestrator.getServer().restoreProfile(FileLocation.of(profile));

    orchestrator.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    orchestrator.getServer().associateProjectToQualityProfile(PROJECT_KEY, "css", "rules");

    SonarScanner scanner = CssTestsUtils.createScanner(PROJECT_KEY);
    scanner.setProperty("sonar.exclusions", "**/file-with-parsing-error-excluded.css");
    scanner.setProperty("sonar.html.file.suffixes", ".htm");
    buildResult = orchestrator.executeBuild(scanner);
  }

  @Test
  void parsing_error_not_on_excluded_files() {
    assertThat(buildResult.getLogs())
      .doesNotMatch("(?s).*ERROR: Failed to parse file:\\S*file-with-parsing-error-excluded\\.css.*")
      .matches("(?s).*ERROR: Failed to parse file:\\S*file-with-parsing-error\\.css, line 1, Unclosed block.*");
  }

  // see https://github.com/SonarSource/sonar-css/issues/235
  @Test
  void parsing_error_on_less_selector_without_leading_space() {
    // there should not be parse error: this comes from a bug reported to transitive dependency postcss-less (https://github.com/shellscape/postcss-less/issues/146)
    assertThat(buildResult.getLogs())
      .matches("(?s).*ERROR: Failed to parse file:\\S*file-with-parsing-error\\.less, line 4, Unknown word.*");
  }

  @Test
  void issue_list() {
    SearchRequest request = new SearchRequest();
    request.setComponentKeys(Collections.singletonList(PROJECT_KEY));
    List<Issue> issuesList = newWsClient(orchestrator).issues().search(request).getIssuesList().stream()
      .filter(i -> i.getRule().startsWith("css:"))
      .collect(Collectors.toList());

    assertThat(issuesList).extracting(Issue::getRule, Issue::getComponent).containsExactlyInAnyOrder(
      tuple("css:S4662", "css-issues-project:src/cssModules.css"),
      tuple("css:S4667", "css-issues-project:src/empty1.css"),
      tuple("css:S4667", "css-issues-project:src/empty2.less"),
      tuple("css:S4667", "css-issues-project:src/empty3.scss"),
      tuple("css:S1128", "css-issues-project:src/file1.css"),
      tuple("css:S1116", "css-issues-project:src/file1.css"),
      tuple("css:S4664", "css-issues-project:src/file1.css"),
      tuple("css:S4660", "css-issues-project:src/file1.css"),
      tuple("css:S4659", "css-issues-project:src/file1.css"),
      tuple("css:S4647", "css-issues-project:src/file1.css"),
      tuple("css:S4663", "css-issues-project:src/file1.css"),
      tuple("css:S4652", "css-issues-project:src/file1.css"),
      tuple("css:S4656", "css-issues-project:src/file1.css"),
      tuple("css:S4649", "css-issues-project:src/file1.css"),
      tuple("css:S4648", "css-issues-project:src/file1.css"),
      tuple("css:S4654", "css-issues-project:src/file1.css"),
      tuple("css:S4657", "css-issues-project:src/file1.css"),
      tuple("css:S4650", "css-issues-project:src/file1.css"),
      tuple("css:S4653", "css-issues-project:src/file1.css"),
      tuple("css:S4668", "css-issues-project:src/file1.css"),
      tuple("css:S4654", "css-issues-project:src/file1.css"),
      tuple("css:S4651", "css-issues-project:src/file1.css"),
      tuple("css:S4666", "css-issues-project:src/file1.css"),
      tuple("css:S4670", "css-issues-project:src/file1.css"),
      tuple("css:S4662", "css-issues-project:src/file1.css"),
      tuple("css:S4655", "css-issues-project:src/file1.css"),
      tuple("css:S4658", "css-issues-project:src/file1.css"),
      tuple("css:S4661", "css-issues-project:src/file1.css"),
      tuple("css:S1128", "css-issues-project:src/file2.less"),
      tuple("css:S1116", "css-issues-project:src/file2.less"),
      tuple("css:S4664", "css-issues-project:src/file2.less"),
      tuple("css:S4660", "css-issues-project:src/file2.less"),
      tuple("css:S4659", "css-issues-project:src/file2.less"),
      tuple("css:S4647", "css-issues-project:src/file2.less"),
      tuple("css:S4663", "css-issues-project:src/file2.less"),
      tuple("css:S4652", "css-issues-project:src/file2.less"),
      tuple("css:S4656", "css-issues-project:src/file2.less"),
      tuple("css:S4649", "css-issues-project:src/file2.less"),
      tuple("css:S4648", "css-issues-project:src/file2.less"),
      tuple("css:S4654", "css-issues-project:src/file2.less"),
      tuple("css:S4657", "css-issues-project:src/file2.less"),
      tuple("css:S4650", "css-issues-project:src/file2.less"),
      tuple("css:S4653", "css-issues-project:src/file2.less"),
      tuple("css:S4651", "css-issues-project:src/file2.less"),
      tuple("css:S4666", "css-issues-project:src/file2.less"),
      tuple("css:S4670", "css-issues-project:src/file2.less"),
      tuple("css:S4662", "css-issues-project:src/file2.less"),
      tuple("css:S4655", "css-issues-project:src/file2.less"),
      tuple("css:S4658", "css-issues-project:src/file2.less"),
      tuple("css:S4661", "css-issues-project:src/file2.less"),
      tuple("css:S1128", "css-issues-project:src/file3.scss"),
      tuple("css:S1116", "css-issues-project:src/file3.scss"),
      tuple("css:S4664", "css-issues-project:src/file3.scss"),
      tuple("css:S4660", "css-issues-project:src/file3.scss"),
      tuple("css:S4659", "css-issues-project:src/file3.scss"),
      tuple("css:S4647", "css-issues-project:src/file3.scss"),
      tuple("css:S4663", "css-issues-project:src/file3.scss"),
      tuple("css:S4652", "css-issues-project:src/file3.scss"),
      tuple("css:S4656", "css-issues-project:src/file3.scss"),
      tuple("css:S4649", "css-issues-project:src/file3.scss"),
      tuple("css:S4648", "css-issues-project:src/file3.scss"),
      tuple("css:S4654", "css-issues-project:src/file3.scss"),
      tuple("css:S4657", "css-issues-project:src/file3.scss"),
      tuple("css:S4650", "css-issues-project:src/file3.scss"),
      tuple("css:S4653", "css-issues-project:src/file3.scss"),
      tuple("css:S4651", "css-issues-project:src/file3.scss"),
      tuple("css:S4666", "css-issues-project:src/file3.scss"),
      tuple("css:S4670", "css-issues-project:src/file3.scss"),
      tuple("css:S4662", "css-issues-project:src/file3.scss"),
      tuple("css:S4655", "css-issues-project:src/file3.scss"),
      tuple("css:S4658", "css-issues-project:src/file3.scss"),
      tuple("css:S4661", "css-issues-project:src/file3.scss"),
      tuple("css:S1116", "css-issues-project:src/file5.htm"),
      tuple("css:S1116", "css-issues-project:src/file6.vue"),
      tuple("css:S5362", "css-issues-project:src/file1.css"),
      tuple("css:S5362", "css-issues-project:src/file1.css"),
      tuple("css:S5362", "css-issues-project:src/file1.css"),
      tuple("css:S5362", "css-issues-project:src/file2.less"),
      tuple("css:S5362", "css-issues-project:src/file2.less"),
      tuple("css:S5362", "css-issues-project:src/file2.less"),
      tuple("css:S5362", "css-issues-project:src/file3.scss"),
      tuple("css:S5362", "css-issues-project:src/file3.scss"),
      tuple("css:S5362", "css-issues-project:src/file3.scss")
    );
  }

}
