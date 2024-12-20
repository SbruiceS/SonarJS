/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2022 SonarSource SA
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
package org.sonar.plugins.javascript.eslint.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.javascript.eslint.PluginInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheKeyTest {

  private final static InputFile inputFile = mock(InputFile.class);

  @BeforeEach
  void setUp() {
    when(inputFile.key()).thenReturn("fileKey");
  }

  @Test
  void test_no_ucfg_version_in_key() {
    PluginInfo.setUcfgPluginVersion(null);
    assertThat(CacheKey.forFile(inputFile)).hasToString("jssecurity:ucfgs:fileKey");
  }

  @Test
  void test_ucfg_version_in_key() {
    PluginInfo.setUcfgPluginVersion("ucfg_version");
    assertThat(CacheKey.forFile(inputFile)).hasToString("jssecurity:ucfgs:ucfg_version:fileKey");
  }
}
