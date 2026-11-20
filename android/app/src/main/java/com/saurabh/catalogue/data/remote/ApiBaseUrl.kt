package com.saurabh.catalogue.data.remote

import javax.inject.Qualifier

/** Marks the backend base URL so it can't be confused with any other HttpUrl in the graph. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApiBaseUrl
