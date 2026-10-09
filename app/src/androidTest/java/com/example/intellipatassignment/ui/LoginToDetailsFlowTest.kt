package com.example.intellipatassignment.ui

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.intellipatassignment.MainActivity
import com.example.intellipatassignment.data.remote.MockServerPrefs
import com.example.intellipatassignment.domain.usecase.LogoutUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class LoginToDetailsFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    @Before
    fun startLoggedOutWithPristineMockServer() {
        runBlocking { GlobalContext.get().get<LogoutUseCase>().invoke() }
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(MockServerPrefs.NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun loginOpenCourseAndCompleteALesson() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNode(hasSetTextAction() and hasText("Email")).performTextInput("user@intellipat.com")
            compose.onNode(hasSetTextAction() and hasText("Password")).performTextInput("Password1")
            compose.onNodeWithText("Login").performClick()

            compose.waitUntilAtLeastOneExists(hasText("Python Programming"), TIMEOUT_MS)
            compose.onNodeWithText("Python Programming").assertIsDisplayed()

            compose.onAllNodesWithText("Continue")[0].performClick()
            compose.waitUntilAtLeastOneExists(hasText("13 of 20 lessons completed"), TIMEOUT_MS)

            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Mark complete"))
            compose.onAllNodesWithText("Mark complete")[0].performClick()
            compose.onNodeWithText("Mark lesson as completed?").assertIsDisplayed()
            compose.onNodeWithText("Confirm").performClick()

            compose.onNode(hasScrollAction()).performScrollToIndex(0)
            compose.waitUntilAtLeastOneExists(hasText("14 of 20 lessons completed"), TIMEOUT_MS)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
