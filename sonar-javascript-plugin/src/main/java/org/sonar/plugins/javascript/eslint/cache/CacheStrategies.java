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

import java.io.IOException;
import javax.annotation.Nullable;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.javascript.eslint.AnalysisMode;

import static org.sonar.plugins.javascript.eslint.cache.CacheStrategy.noCache;
import static org.sonar.plugins.javascript.eslint.cache.CacheStrategy.readAndWrite;
import static org.sonar.plugins.javascript.eslint.cache.CacheStrategy.writeOnly;

public class CacheStrategies {

  private static final Logger LOG = Loggers.get(CacheStrategies.class);

  private static final CacheReporter REPORTER = new CacheReporter();

  private CacheStrategies() {
  }

  private static boolean isRuntimeApiCompatible(SensorContext context) {
    var isVersionValid = context.runtime().getApiVersion().isGreaterThanOrEqual(Version.create(9, 4));
    var isProductValid = context.runtime().getProduct() != SonarProduct.SONARLINT;
    return isVersionValid && isProductValid;
  }

  static String getLogMessage(CacheStrategy cacheStrategy, InputFile inputFile, @Nullable String reason) {
    var logBuilder = new StringBuilder("Cache strategy set to '");
    logBuilder.append(cacheStrategy.getName()).append("' for file '").append(inputFile).append("'");
    if (reason != null) {
      logBuilder.append(" as ").append(reason);
    }
    return logBuilder.toString();
  }

  public static CacheStrategy getStrategyFor(SensorContext context, InputFile inputFile) {
    if (!isRuntimeApiCompatible(context)) {
      var strategy = noCache();
      REPORTER.logAndIncrement(strategy, inputFile, MissReason.RUNTIME_API_INCOMPATIBLE);
      return strategy;
    }

    var cacheKey = CacheKey.forFile(inputFile);
    var serialization = new UCFGFilesSerialization(context, cacheKey);

    if (!AnalysisMode.isRuntimeApiCompatible(context) || !context.canSkipUnchangedFiles()) {
      var strategy = writeOnly(serialization);
      REPORTER.logAndIncrement(strategy, inputFile, MissReason.ANALYSIS_MODE_INELIGIBLE);
      return strategy;
    }

    if (inputFile.status() != InputFile.Status.SAME) {
      var strategy = writeOnly(serialization);
      REPORTER.logAndIncrement(strategy, inputFile, MissReason.FILE_CHANGED);
      return strategy;
    }

    if (!serialization.isInCache()) {
      var strategy = writeOnly(serialization);
      REPORTER.logAndIncrement(strategy, inputFile, MissReason.FILE_NOT_IN_CACHE);
      return strategy;
    }

    if (!writeFilesFromCache(serialization)) {
      var strategy = writeOnly(serialization);
      REPORTER.logAndIncrement(strategy, inputFile, MissReason.CACHE_CORRUPTED);
      return strategy;
    }

    var strategy = readAndWrite(serialization);
    REPORTER.logAndIncrement(strategy, inputFile, null);
    return strategy;
  }

  static boolean writeFilesFromCache(UCFGFilesSerialization serialization) {
    try {
      serialization.readFromCache();
      serialization.copyFromPrevious();
      return true;
    } catch (IOException e) {
      LOG.error("Failure when reading cache entry", e);
      return false;
    }
  }

  public static void reset() {
    REPORTER.reset();
  }

  public static void logReport() {
    REPORTER.logReport();
  }

  enum MissReason {
    RUNTIME_API_INCOMPATIBLE("the runtime API is not compatible"),
    CACHE_DISABLED("cache is disabled"),
    ANALYSIS_MODE_INELIGIBLE("current analysis requires all files to be analyzed"),
    FILE_CHANGED("the current file is changed"),
    FILE_NOT_IN_CACHE("the current file is not cached"),
    CACHE_CORRUPTED("the cache is corrupted");

    private final String description;

    MissReason(String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

}
