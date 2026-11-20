package com.saurabh.catalogue.ui.enquiry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import com.saurabh.catalogue.domain.model.EnquiryConfirmation
import com.saurabh.catalogue.domain.model.EnquiryRequest
import com.saurabh.catalogue.domain.repository.EnquiryRepository
import com.saurabh.catalogue.domain.usecase.SubmitEnquiryUseCase
import com.saurabh.catalogue.navigation.RouteArgs
import com.saurabh.catalogue.ui.theme.CatalogueTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for the enquiry acceptance criterion: complete the form → submit → "Thank you".
 * Uses the real ViewModel and use case with an in-memory repository (no Hilt, no network).
 * Hosts the two screens the way AppNavHost does: Thank you replaces the form once sent.
 */
@RunWith(AndroidJUnit4::class)
class EnquiryFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class RecordingRepository(var error: AppError? = null) : EnquiryRepository {
        val submitted = mutableListOf<EnquiryRequest>()
        override suspend fun submit(request: EnquiryRequest): Outcome<EnquiryConfirmation> {
            submitted += request
            return error?.let { Outcome.Failure(it) } ?: Outcome.Success(EnquiryConfirmation("e-1"))
        }
    }

    private fun launchFlow(repository: RecordingRepository) {
        val viewModel = EnquiryViewModel(
            SavedStateHandle(mapOf(RouteArgs.ENQUIRY_PRODUCT_ID to "3", RouteArgs.ENQUIRY_PRODUCT_TITLE to "Mens Cotton Jacket")),
            SubmitEnquiryUseCase(repository),
        )
        composeRule.setContent {
            var sentTitle by remember { mutableStateOf<String?>(null) }
            CatalogueTheme {
                val title = sentTitle
                if (title == null) {
                    EnquiryScreen(onBack = {}, onSent = { sentTitle = it }, viewModel = viewModel)
                } else {
                    ThankYouScreen(productTitle = title, onDone = {})
                }
            }
        }
    }

    @Test
    fun completingTheFormShowsThankYou() {
        val repository = RecordingRepository()
        launchFlow(repository)

        composeRule.onNodeWithTag("enquiry_name").performTextInput("Asha Rao")
        composeRule.onNodeWithTag("enquiry_email").performTextInput("asha@example.com")
        composeRule.onNodeWithTag("enquiry_message").performTextInput("Do you ship to Pune?")
        composeRule.onNodeWithTag("enquiry_submit").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists("thank_you")
        }
        composeRule.onNodeWithText("Thank you!").assertIsDisplayed()
        assertEquals("Asha Rao", repository.submitted.single().name)
    }

    @Test
    fun missingRequiredFieldsShowErrorsAndStayOnTheForm() {
        val repository = RecordingRepository()
        launchFlow(repository)

        composeRule.onNodeWithTag("enquiry_submit").performScrollTo().performClick()

        composeRule.onNodeWithTag("enquiry_name_error").assertIsDisplayed()
        composeRule.onNodeWithTag("enquiry_email_error").assertIsDisplayed()
        assertTrue(repository.submitted.isEmpty())
    }

    @Test
    fun failedSendShowsErrorAndNoThankYou() {
        val repository = RecordingRepository(error = AppError.Network)
        launchFlow(repository)

        composeRule.onNodeWithTag("enquiry_name").performTextInput("Asha Rao")
        composeRule.onNodeWithTag("enquiry_email").performTextInput("asha@example.com")
        composeRule.onNodeWithTag("enquiry_submit").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists("enquiry_submit_error")
        }
        composeRule.onNodeWithTag("enquiry_submit_error").assertIsDisplayed()
        assertTrue(!composeRule.onAllNodesWithTagExists("thank_you"))
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagExists(tag: String): Boolean =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
}
