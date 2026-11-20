package com.saurabh.catalogue.data.repository

import com.saurabh.catalogue.data.remote.CatalogueApi
import com.saurabh.catalogue.data.remote.dto.EnquiryRequestDto
import com.saurabh.catalogue.di.NetworkModule
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real Retrofit + kotlinx.serialization + error-mapping stack against a local fake server.
 * Uses NetworkModule's own provider functions, so the test runs the exact production configuration.
 * No emulator, no backend: runs on the JVM in a second.
 */
class ProductRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var api: CatalogueApi
    private lateinit var repository: ProductRepositoryImpl
    private val json = NetworkModule.provideJson()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val baseUrl = server.url("/")
        api = NetworkModule.provideCatalogueApi(NetworkModule.provideRetrofit(baseUrl, OkHttpClient(), json))
        repository = ProductRepositoryImpl(api, baseUrl, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun respond(code: Int, body: String) {
        server.enqueue(
            MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body),
        )
    }

    @Test
    fun `featured calls the featured endpoint and maps products`() = runTest {
        respond(200, """{"products":[$PRODUCT_JSON]}""")

        val result = repository.featured()

        assertEquals("/v1/products/featured", server.takeRequest().path)
        val product = (result as Outcome.Success).value.single()
        assertEquals("3", product.id)
        assertEquals("Mens Cotton Jacket", product.title)
        assertEquals(5599L, product.price.amountMinor)
        assertEquals("mens-clothing", product.category.slug)
        assertEquals(4.7, product.rating.average, 0.0)
    }

    @Test
    fun `relative image URLs are resolved against the API base URL`() = runTest {
        respond(200, """{"products":[$PRODUCT_JSON]}""")

        val product = (repository.featured() as Outcome.Success).value.single()

        assertEquals(server.url("/v1/products/3/image").toString(), product.imageUrl)
    }

    @Test
    fun `search sends the term as an encoded query parameter`() = runTest {
        respond(200, """{"query":"mens jacket","products":[]}""")

        val result = repository.search("mens jacket")

        val request = server.takeRequest()
        assertEquals("/v1/products", request.requestUrl!!.encodedPath)
        assertEquals("mens jacket", request.requestUrl!!.queryParameter("q"))
        assertEquals(emptyList<Any>(), (result as Outcome.Success).value)
    }

    @Test
    fun `category page maps top rated and other products separately`() = runTest {
        respond(
            200,
            """{"category":{"slug":"mens-clothing","name":"Men's clothing"},"topRated":[$PRODUCT_JSON],"others":[]}""",
        )

        val page = (repository.categoryProducts("mens-clothing") as Outcome.Success).value

        assertEquals("/v1/categories/mens-clothing/products", server.takeRequest().path)
        assertEquals(listOf("3"), page.topRated.map { it.id })
        assertTrue(page.others.isEmpty())
    }

    @Test
    fun `unknown fields from a newer backend are ignored`() = runTest {
        respond(200, """{"products":[$PRODUCT_JSON],"nextPage":2}""")

        assertTrue(repository.featured() is Outcome.Success)
    }

    @Test
    fun `404 maps to NotFound`() = runTest {
        respond(404, """{"error":{"code":"PRODUCT_NOT_FOUND","message":"Product not found"}}""")

        assertEquals(Outcome.Failure(AppError.NotFound), repository.product("999"))
    }

    @Test
    fun `400 maps to Validation with field errors`() = runTest {
        respond(
            400,
            """{"error":{"code":"VALIDATION_FAILED","message":"Some fields are invalid",
               "details":[{"field":"q","message":"Search term is required"}]}}""",
        )

        assertEquals(
            Outcome.Failure(AppError.Validation(mapOf("q" to "Search term is required"))),
            repository.search(" "),
        )
    }

    @Test
    fun `429 maps to RateLimited`() = runTest {
        respond(429, """{"error":{"code":"RATE_LIMITED","message":"Too many"}}""")

        assertEquals(Outcome.Failure(AppError.RateLimited), repository.featured())
    }

    @Test
    fun `502 maps to Server with the backend message`() = runTest {
        respond(502, """{"error":{"code":"DATA_SOURCE_UNAVAILABLE","message":"Products are temporarily unavailable"}}""")

        assertEquals(
            Outcome.Failure(AppError.Server("Products are temporarily unavailable")),
            repository.featured(),
        )
    }

    @Test
    fun `5xx without a JSON body still maps to Server`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))

        assertEquals(Outcome.Failure(AppError.Server(null)), repository.featured())
    }

    @Test
    fun `dropped connection maps to Network`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(Outcome.Failure(AppError.Network), repository.featured())
    }

    @Test
    fun `malformed JSON maps to Unexpected`() = runTest {
        respond(200, """{"products":[{"id":3}]}""")

        assertEquals(Outcome.Failure(AppError.Unexpected), repository.featured())
    }

    @Test
    fun `enquiry is POSTed as JSON without null optional fields`() = runTest {
        respond(201, """{"enquiryId":"abc","status":"sent"}""")

        val response = api.submitEnquiry(EnquiryRequestDto(productId = "3", name = "Asha", email = "asha@example.com"))

        val request = server.takeRequest()
        val body = request.body.readUtf8()
        assertEquals("POST", request.method)
        assertEquals("/v1/enquiries", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertTrue(body.contains("\"productId\":\"3\""))
        assertFalse("null optionals should be omitted", body.contains("phone"))
        assertEquals("abc", response.enquiryId)
    }

    private companion object {
        const val PRODUCT_JSON = """
            {"id":"3","title":"Mens Cotton Jacket","description":"Warm",
             "price":{"amount":5599,"currency":"USD"},"imageUrl":"/v1/products/3/image",
             "category":{"slug":"mens-clothing","name":"Men's clothing"},
             "rating":{"average":4.7,"count":500}}
        """
    }
}
