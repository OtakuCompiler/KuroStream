package com.kurostream.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

private val MEDIA_TYPE = "application/json".toMediaType()

class GsonConverterFactory private constructor(private val gson: Gson) : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val adapter = gson.getAdapter(TypeToken.get(type))
        return Converter<ResponseBody, Any> { body ->
            adapter.fromJson(body.charStream())
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody> {
        val adapter = gson.getAdapter(TypeToken.get(type))
        return Converter<Any, RequestBody> { value ->
            gson.toJson(value).toRequestBody(MEDIA_TYPE)
        }
    }

    companion object {
        fun create(): GsonConverterFactory = GsonConverterFactory(Gson())
        fun create(gson: Gson): GsonConverterFactory = GsonConverterFactory(gson)
    }
}
