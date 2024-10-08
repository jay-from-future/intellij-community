// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.mac.MacMainFrameDecorator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author ignatov
 */
@ApiStatus.Internal
public abstract class MacWindowActionBase extends AnAction implements DumbAware {
  @Override
  public void update(final @NotNull AnActionEvent e) {
    Presentation p = e.getPresentation();
    p.setVisible(SystemInfo.isMac);

    if (SystemInfo.isMac) {
      Project project = e.getProject();
      if (project != null) {
        JFrame frame = WindowManager.getInstance().getFrame(project);
        if (frame != null) {
          JRootPane pane = frame.getRootPane();
          p.setEnabled(pane != null && pane.getClientProperty(MacMainFrameDecorator.FULL_SCREEN) == null);
        }
      }
    }
    else {
      p.setEnabled(false);
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.EDT;
  }
}
