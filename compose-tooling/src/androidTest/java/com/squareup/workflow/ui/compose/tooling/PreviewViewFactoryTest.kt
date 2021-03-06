/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("TestFunctionName", "PrivatePropertyName")

package com.squareup.workflow.ui.compose.tooling

import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithText
import androidx.ui.tooling.preview.Preview
import com.squareup.workflow.ui.ViewEnvironmentKey
import com.squareup.workflow.ui.compose.WorkflowRendering
import com.squareup.workflow.ui.compose.composedViewFactory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreviewViewFactoryTest {

  @Rule @JvmField val composeRule = createComposeRule()

  @Test fun singleChild() {
    composeRule.setContent {
      ParentWithOneChildPreview()
    }

    onNodeWithText("one").assertIsDisplayed()
    onNodeWithText("two").assertIsDisplayed()
  }

  @Test fun twoChildren() {
    composeRule.setContent {
      ParentWithTwoChildrenPreview()
    }

    onNodeWithText("one").assertIsDisplayed()
    onNodeWithText("two").assertIsDisplayed()
    onNodeWithText("three").assertIsDisplayed()
  }

  @Test fun recursive() {
    composeRule.setContent {
      ParentRecursivePreview()
    }

    onNodeWithText("one").assertIsDisplayed()
    onNodeWithText("two").assertIsDisplayed()
    onNodeWithText("three").assertIsDisplayed()
  }

  @Test fun modifierIsApplied() {
    composeRule.setContent {
      ParentWithModifier()
    }

    // The view factory will be rendered with size (0,0), so it should be reported as not displayed.
    onNodeWithText("one").assertIsNotDisplayed()
    onNodeWithText("two").assertIsNotDisplayed()
  }

  @Test fun placeholderModifierIsApplied() {
    composeRule.setContent {
      ParentWithPlaceholderModifier()
    }

    // The child will be rendered with size (0,0), so it should be reported as not displayed.
    onNodeWithText("one").assertIsDisplayed()
    onNodeWithText("two").assertIsNotDisplayed()
  }

  @Test fun customViewEnvironment() {
    composeRule.setContent {
      ParentConsumesCustomKeyPreview()
    }

    onNodeWithText("foo").assertIsDisplayed()
  }

  private val ParentWithOneChild =
    composedViewFactory<Pair<String, String>> { rendering, environment ->
      Column {
        Text(rendering.first)
        WorkflowRendering(rendering.second, environment)
      }
    }

  @Preview @Composable private fun ParentWithOneChildPreview() {
    ParentWithOneChild.preview(Pair("one", "two"))
  }

  private val ParentWithTwoChildren =
    composedViewFactory<Triple<String, String, String>> { rendering, environment ->
      Column {
        WorkflowRendering(rendering.first, environment)
        Text(rendering.second)
        WorkflowRendering(rendering.third, environment)
      }
    }

  @Preview @Composable private fun ParentWithTwoChildrenPreview() {
    ParentWithTwoChildren.preview(Triple("one", "two", "three"))
  }

  data class RecursiveRendering(
    val text: String,
    val child: RecursiveRendering? = null
  )

  private val ParentRecursive = composedViewFactory<RecursiveRendering> { rendering, environment ->
    Column {
      Text(rendering.text)
      rendering.child?.let { child ->
        WorkflowRendering(rendering = child, viewEnvironment = environment)
      }
    }
  }

  @Preview @Composable private fun ParentRecursivePreview() {
    ParentRecursive.preview(
        RecursiveRendering(
            text = "one",
            child = RecursiveRendering(
                text = "two",
                child = RecursiveRendering(text = "three")
            )
        )
    )
  }

  @Preview @Composable private fun ParentWithModifier() {
    ParentWithOneChild.preview(
        Pair("one", "two"),
        modifier = Modifier.size(0.dp)
    )
  }

  @Preview @Composable private fun ParentWithPlaceholderModifier() {
    ParentWithOneChild.preview(
        Pair("one", "two"),
        placeholderModifier = Modifier.size(0.dp)
    )
  }

  object TestEnvironmentKey : ViewEnvironmentKey<String>(String::class) {
    override val default: String get() = error("Not specified")
  }

  private val ParentConsumesCustomKey = composedViewFactory<Unit> { _, environment ->
    Text(environment[TestEnvironmentKey])
  }

  @Preview @Composable private fun ParentConsumesCustomKeyPreview() {
    ParentConsumesCustomKey.preview(Unit) {
      it + (TestEnvironmentKey to "foo")
    }
  }
}
