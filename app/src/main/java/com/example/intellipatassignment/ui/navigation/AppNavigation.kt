package com.example.intellipatassignment.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.intellipatassignment.ui.courses.CoursesRoute
import com.example.intellipatassignment.ui.details.CourseDetailsRoute
import com.example.intellipatassignment.ui.login.LoginRoute
import kotlinx.serialization.Serializable

@Serializable data object LoginDestination
@Serializable data object CoursesDestination
@Serializable data class CourseDetailsDestination(val courseId: Int)

@Composable
fun AppNavigation(
    isLoggedIn: Boolean,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) CoursesDestination else LoginDestination,
    ) {
        composable<LoginDestination> {
            LoginRoute(onLoggedIn = {
                navController.navigate(CoursesDestination) {
                    popUpTo<LoginDestination> { inclusive = true }
                }
            })
        }
        composable<CoursesDestination> {
            CoursesRoute(
                onCourseClick = { navController.navigate(CourseDetailsDestination(it)) },
                onLoggedOut = {
                    navController.navigate(LoginDestination) {
                        popUpTo<CoursesDestination> { inclusive = true }
                    }
                },
            )
        }
        composable<CourseDetailsDestination> { entry ->
            CourseDetailsRoute(
                courseId = entry.toRoute<CourseDetailsDestination>().courseId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
