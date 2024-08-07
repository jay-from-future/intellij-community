// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4idea.ui.branch.tree

import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.ui.RepositoryChangesBrowserNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.ClientProperty
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.popup.PopupFactoryImpl
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.ui.tree.ui.Control
import com.intellij.ui.tree.ui.DefaultControl
import com.intellij.ui.util.getAvailTextLength
import com.intellij.util.PlatformIcons
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UpdateScaleHelper
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.util.ui.tree.TreeUtil
import git4idea.GitBranch
import git4idea.GitReference
import git4idea.GitTag
import git4idea.branch.GitBranchType
import git4idea.branch.GitRefType
import git4idea.branch.TagsNode
import git4idea.i18n.GitBundle
import git4idea.repo.GitRefUtil
import git4idea.repo.GitRepository
import git4idea.ui.branch.GitBranchManager
import git4idea.ui.branch.GitBranchesClippedNamesCache
import git4idea.ui.branch.popup.GitBranchesTreePopupBase
import git4idea.ui.branch.tree.GitBranchesTreeModel.RefUnderRepository
import git4idea.ui.branch.tree.GitBranchesTreeUtil.canHighlight
import icons.DvcsImplIcons
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreePath

abstract class GitBranchesTreeRenderer(
  protected val project: Project,
  private val treeModel: GitBranchesTreeModel,
  private val selectedRepository: GitRepository?,
  repositories: List<GitRepository>,
  private val favoriteToggleOnClickSupported: Boolean = true,
) : TreeCellRenderer {
  private val colorManager = RepositoryChangesBrowserNode.getColorManager(project)

  private val updateScaleHelper = UpdateScaleHelper()

  protected val affectedRepositories = selectedRepository?.let(::listOf) ?: repositories

  private fun getBranchNameClipper(treeNode: Any?): SimpleColoredComponent.FragmentTextClipper? =
    GitBranchesTreeRendererClipper.create(project, treeNode)

  fun getLeftTreeIconRenderer(path: TreePath): Control? {
    val lastComponent = path.lastPathComponent
    val defaultIcon = getNodeIcon(lastComponent, false) ?: return null
    val selectedIcon = getNodeIcon(lastComponent, true) ?: return null

    return DefaultControl(defaultIcon, defaultIcon, selectedIcon, selectedIcon)
  }

  fun getIcon(treeNode: Any?, isSelected: Boolean): Icon? {
    val value = treeNode ?: return null
    return when (value) {
      is GitBranchesTreeModel.BranchesPrefixGroup -> PlatformIcons.FOLDER_ICON
      is RefUnderRepository -> getBranchIcon(value.ref, listOf(value.repository), isSelected)
      is GitReference -> getBranchIcon(value, affectedRepositories, isSelected)
      else -> null
    }
  }

  private fun getBranchIcon(reference: GitReference, repositories: List<GitRepository>, isSelected: Boolean): Icon {
    val isCurrent =
      selectedRepository?.let { GitRefUtil.getCurrentReference(it) == reference }
      ?: repositories.all { GitRefUtil.getCurrentReference(it) == reference }

    val branchManager = project.service<GitBranchManager>()
    val isFavorite =
      selectedRepository?.let { branchManager.isFavorite(GitRefType.of(reference), it, reference.name) }
      ?: repositories.all { branchManager.isFavorite(GitRefType.of(reference), it, reference.name) }

    return when {
      isSelected && isFavorite -> AllIcons.Nodes.Favorite
      isSelected && favoriteToggleOnClickSupported -> AllIcons.Nodes.NotFavoriteOnHover
      isCurrent && isFavorite -> DvcsImplIcons.CurrentBranchFavoriteLabel
      isCurrent -> DvcsImplIcons.CurrentBranchLabel
      isFavorite -> AllIcons.Nodes.Favorite
      reference is GitTag -> DvcsImplIcons.BranchLabel
      else -> AllIcons.Vcs.BranchNode
    }
  }

  private fun getNodeIcon(treeNode: Any?, isSelected: Boolean): Icon? {
    val value = treeNode ?: return null
    return when (value) {
      is PopupFactoryImpl.ActionItem -> value.getIcon(isSelected)
      is GitRepository -> RepositoryChangesBrowserNode.getRepositoryIcon(value, colorManager)
      is GitBranchesTreeModel.TopLevelRepository -> RepositoryChangesBrowserNode.getRepositoryIcon(value.repository, colorManager)
      else -> null
    }
  }

  protected val mainIconComponent = JLabel().apply {
    ClientProperty.put(this, MAIN_ICON, true)
    border = JBUI.Borders.emptyRight(4)  // 6 px in spec, but label width is differed
  }

  protected val mainTextComponent = SimpleColoredComponent().apply {
    isOpaque = false
    border = JBUI.Borders.empty()
  }

  abstract val mainPanel: BorderLayoutPanel

  final override fun getTreeCellRendererComponent(tree: JTree,
                                                  value: Any?,
                                                  selected: Boolean,
                                                  expanded: Boolean,
                                                  leaf: Boolean,
                                                  row: Int,
                                                  hasFocus: Boolean): Component {
    val userObject = TreeUtil.getUserObject(value)
    // render separator text in accessible mode
    if (userObject is SeparatorWithText) return userObject

    mainIconComponent.apply {
      icon = getIcon(userObject, selected)
      isVisible = icon != null
    }

    mainTextComponent.apply {
      background = JBUI.CurrentTheme.Tree.background(selected, true)
      foreground = JBUI.CurrentTheme.Tree.foreground(selected, true)

      clear()
      val text = getText(userObject, treeModel, affectedRepositories).orEmpty()

      if (isDisabledActionItem(userObject)) {
        append(text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
      }
      else {
        appendWithClipping(text, getBranchNameClipper(userObject))
      }
    }

    configureTreeCellComponent(tree, userObject, value, selected, expanded, leaf, row, hasFocus)

    if (value != null && canHighlight(project, tree, userObject)) {
      SpeedSearchUtil.applySpeedSearchHighlightingFiltered(tree, value, mainTextComponent, true, selected)
    }

    if (updateScaleHelper.saveScaleAndUpdateUIIfChanged(mainPanel)) {
      tree.rowHeight = GitBranchesTreePopupBase.treeRowHeight
    }

    return mainPanel
  }

  abstract fun configureTreeCellComponent(tree: JTree, userObject: Any?, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean)

  companion object {
    @JvmField
    internal val MAIN_ICON = Key.create<Boolean>("MAIN_ICON")

    internal fun getText(treeNode: Any?, model: GitBranchesTreeModel, repositories: List<GitRepository>): @NlsSafe String? {
      val value = treeNode ?: return null
      return when (value) {
        GitBranchesTreeModel.RecentNode -> {
          when (model) {
            is GitBranchesTreeSelectedRepoModel -> GitBundle.message("group.Git.Recent.Branch.in.repo.title",
                                                                     DvcsUtil.getShortRepositoryName(model.selectedRepository))
            else -> GitBundle.message("group.Git.Recent.Branch.title")
          }
        }
        TagsNode -> {
          when {
            model is GitBranchesTreeSelectedRepoModel -> GitBundle.message("branches.tags.in.repo",
                                                                           DvcsUtil.getShortRepositoryName(model.selectedRepository))
            repositories.size > 1 -> GitBundle.message("common.tags")
            else -> GitBundle.message("group.Git.Tags.title")
          }
        }
        GitBranchType.LOCAL -> {
          when {
            model is GitBranchesTreeSelectedRepoModel -> GitBundle.message("branches.local.branches.in.repo",
                                                                           DvcsUtil.getShortRepositoryName(model.selectedRepository))
            repositories.size > 1 -> GitBundle.message("common.local.branches")
            else -> GitBundle.message("group.Git.Local.Branch.title")
          }
        }
        GitBranchType.REMOTE -> {
          when {
            model is GitBranchesTreeSelectedRepoModel -> GitBundle.message("branches.remote.branches.in.repo",
                                                                           DvcsUtil.getShortRepositoryName(model.selectedRepository))
            repositories.size > 1 -> GitBundle.message("common.remote.branches")
            else -> GitBundle.message("group.Git.Remote.Branch.title")
          }
        }
        is GitBranchesTreeModel.BranchesPrefixGroup -> value.prefix.last()
        is GitRepository -> DvcsUtil.getShortRepositoryName(value)
        is GitBranchesTreeModel.RefTypeUnderRepository -> {
          when (value.type) {
            GitBranchesTreeModel.RecentNode -> GitBundle.message("group.Git.Recent.Branch.title")
            GitBranchType.LOCAL -> GitBundle.message("group.Git.Local.Branch.title")
            GitBranchType.REMOTE -> GitBundle.message("group.Git.Remote.Branch.title")
            else -> null
          }
        }
        is RefUnderRepository -> getText(value.ref, model, repositories)
        is GitReference -> if (model.isPrefixGrouping) value.name.split('/').last() else value.name
        is PopupFactoryImpl.ActionItem -> value.text
        is GitBranchesTreeModel.PresentableNode -> value.presentableText
        else -> null
      }
    }
    internal fun isDisabledActionItem(userObject: Any?) = userObject is PopupFactoryImpl.ActionItem && !userObject.isEnabled
  }
}

private class GitBranchesTreeRendererClipper(private val project: Project) : SimpleColoredComponent.FragmentTextClipper {

  override fun clipText(component: SimpleColoredComponent, g2: Graphics2D, fragmentIndex: Int, text: String, availTextWidth: Int): String {
    if (component.fragmentCount > 1) return text
    val clipCache = project.service<GitBranchesClippedNamesCache>()
    return clipCache.getOrCache(text, component.getAvailTextLength(text, availTextWidth))
  }

  companion object {
    fun create(project: Project, treeNode: Any?): SimpleColoredComponent.FragmentTextClipper? {
      if (treeNode is RefUnderRepository ||
          treeNode is GitBranch) {
        return GitBranchesTreeRendererClipper(project)
      }
      return null
    }
  }
}
