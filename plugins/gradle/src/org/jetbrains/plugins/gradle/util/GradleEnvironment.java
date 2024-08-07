// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GradleEnvironment {

  @NonNls public static final boolean DEBUG_GRADLE_HOME_PROCESSING = Boolean.getBoolean("gradle.debug.home.processing");

  public static final class Headless {
    @NonNls public static final String GRADLE_DISTRIBUTION_TYPE = System.getProperty("idea.gradle.distributionType");
    @NonNls public static final String GRADLE_HOME = System.getProperty("idea.gradle.home");
    @NonNls public static final String GRADLE_VM_OPTIONS = System.getProperty("idea.gradle.vmOptions");
    @NonNls public static final String GRADLE_OFFLINE = System.getProperty("idea.gradle.offline");
    @NonNls public static final String GRADLE_SERVICE_DIRECTORY = System.getProperty("idea.gradle.serviceDirectory");
  }

  @ApiStatus.Internal
  public static final class Urls {

    public static final @Nullable String MAVEN_REPOSITORY_URL =
      System.getProperty("idea.gradle.mavenRepositoryUrl", null);

    public static final @NotNull String GRADLE_SERVICES_URL =
      System.getProperty("idea.gradle.servicesUrl", "https://services.gradle.org");
  }
}
