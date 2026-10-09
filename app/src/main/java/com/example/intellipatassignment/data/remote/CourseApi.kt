package com.example.intellipatassignment.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface CourseApi {
    @GET("courses")
    suspend fun getCourses(): List<CourseDto>

    @POST("courses/{courseId}/lessons/{lessonId}/complete")
    suspend fun completeLesson(
        @Path("courseId") courseId: Int,
        @Path("lessonId") lessonId: Int,
    )
}
