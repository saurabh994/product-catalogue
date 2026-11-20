package com.saurabh.catalogue.ui.enquiry

import androidx.lifecycle.SavedStateHandle
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.usecase.EnquiryValidator
import com.saurabh.catalogue.domain.usecase.SubmitEnquiryUseCase
import com.saurabh.catalogue.fakes.FakeEnquiryRepository
import com.saurabh.catalogue.navigation.RouteArgs
import com.saurabh.catalogue.testutil.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EnquiryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeEnquiryRepository()

    private fun createViewModel() = EnquiryViewModel(
        SavedStateHandle(mapOf(RouteArgs.ENQUIRY_PRODUCT_ID to "3", RouteArgs.ENQUIRY_PRODUCT_TITLE to "Mens Cotton Jacket")),
        SubmitEnquiryUseCase(repository),
    )

    private fun EnquiryViewModel.fillValid() {
        onNameChange("Asha Rao")
        onEmailChange("asha@example.com")
    }

    @Test
    fun `shows the product title from the route`() {
        assertEquals("Mens Cotton Jacket", createViewModel().state.value.productTitle)
    }

    @Test
    fun `required fields are validated locally and nothing is sent`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(setOf(EnquiryValidator.NAME, EnquiryValidator.EMAIL), state.fieldErrors.keys)
        assertFalse(state.sent)
        assertTrue(repository.submitted.isEmpty())
    }

    @Test
    fun `invalid email and phone are rejected`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onNameChange("Asha")
        viewModel.onEmailChange("not-an-email")
        viewModel.onPhoneChange("call me")
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        assertEquals(setOf(EnquiryValidator.EMAIL, EnquiryValidator.PHONE), viewModel.state.value.fieldErrors.keys)
    }

    @Test
    fun `valid form is sent trimmed, with blank optionals as null, and marks sent`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.onNameChange("  Asha Rao ")
        viewModel.onEmailChange(" asha@example.com ")
        viewModel.onPhoneChange("   ")
        viewModel.onMessageChange("Do you ship to Pune?")
        viewModel.submit()
        assertTrue(viewModel.state.value.isSubmitting)

        testScheduler.advanceUntilIdle()

        val sent = repository.submitted.single()
        assertEquals("3", sent.productId)
        assertEquals("Asha Rao", sent.name)
        assertEquals("asha@example.com", sent.email)
        assertNull(sent.phone)
        assertEquals("Do you ship to Pune?", sent.message)
        assertTrue(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `backend field errors are shown on the form`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Validation(mapOf("email" to "Enter a valid email address"))
        val viewModel = createViewModel()
        viewModel.fillValid()
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        assertEquals(mapOf("email" to "Enter a valid email address"), viewModel.state.value.fieldErrors)
        assertNull(viewModel.state.value.submitError)
        assertFalse(viewModel.state.value.sent)
    }

    @Test
    fun `a validation error on a non-form field is shown as a send error`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Validation(mapOf("productId" to "Invalid product"))
        val viewModel = createViewModel()
        viewModel.fillValid()
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.submitError is AppError.Validation)
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
    }

    @Test
    fun `send failure keeps the input, shows an error, and retry succeeds`() = runTest(mainDispatcherRule.dispatcher) {
        repository.error = AppError.Server("We could not send your enquiry")
        val viewModel = createViewModel()
        viewModel.fillValid()
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        val failed = viewModel.state.value
        assertEquals(AppError.Server("We could not send your enquiry"), failed.submitError)
        assertEquals("Asha Rao", failed.name)
        assertFalse("no Thank you after a failed send", failed.sent)

        repository.error = null
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value.sent)
        assertNull(viewModel.state.value.submitError)
        assertEquals(2, repository.submitted.size)
    }

    @Test
    fun `editing a field clears its error`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.submit()
        testScheduler.advanceUntilIdle()
        assertTrue(EnquiryValidator.NAME in viewModel.state.value.fieldErrors)

        viewModel.onNameChange("A")

        assertFalse(EnquiryValidator.NAME in viewModel.state.value.fieldErrors)
        assertTrue("other errors stay", EnquiryValidator.EMAIL in viewModel.state.value.fieldErrors)
    }

    @Test
    fun `double tapping submit sends only once`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = createViewModel()
        viewModel.fillValid()
        viewModel.submit()
        viewModel.submit()
        testScheduler.advanceUntilIdle()

        assertEquals(1, repository.submitted.size)
    }
}
