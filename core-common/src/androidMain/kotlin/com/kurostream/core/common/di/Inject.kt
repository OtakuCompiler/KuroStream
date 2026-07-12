package com.kurostream.core.common.di

import javax.inject.Inject as Jsr330Inject
import kotlin.annotation.AnnotationRetention

@Retention(AnnotationRetention.RUNTIME)
actual annotation class Inject constructor() : Jsr330Inject