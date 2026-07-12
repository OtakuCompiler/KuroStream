package com.kurostream.core.common.di

import kotlin.annotation.AnnotationRetention
import kotlin.annotation.RetentionPolicy

@Retention(AnnotationRetention.BINARY)
expect annotation class Inject constructor()