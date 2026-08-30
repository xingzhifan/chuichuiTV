package com.chuichui.video

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chuichui.video.ui.Categories
import com.chuichui.video.ui.DetailScreen
import com.chuichui.video.ui.PlayScreen
import com.chuichui.video.ui.SearchScreen
import com.chuichui.video.ui.SourcesScreen
import com.chuichui.video.ui.VodListScreen
import com.chuichui.video.play.PlayRequest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "categories") {

                    composable("categories") {
                        Categories(
                            onOpenCategory = { typeId, title ->
                                nav.navigate("vodList?typeId=${Uri.encode(typeId)}&title=${Uri.encode(title)}")
                            },
                            onOpenSearch = { nav.navigate("search") },
                            onOpenSettings = { nav.navigate("settings") }
                        )
                    }

                    composable("settings") {
                        SourcesScreen(onClose = { nav.popBackStack() })
                    }

                    composable("search") {
                        SearchScreen(
                            onOpenVod = { vodId, name ->
                                nav.navigate("detail?vodId=${Uri.encode(vodId)}&title=${Uri.encode(name)}")
                            }
                        )
                    }

                    composable(
                        route = "vodList?typeId={typeId}&title={title}",
                        arguments = listOf(
                            navArgument("typeId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                        )
                    ) { back ->
                        VodListScreen(
                            typeId = back.arguments?.getString("typeId").orEmpty(),
                            title = back.arguments?.getString("title").orEmpty(),
                            onOpenVod = { vodId, name ->
                                nav.navigate("detail?vodId=${Uri.encode(vodId)}&title=${Uri.encode(name)}")
                            }
                        )
                    }

                    composable(
                        route = "detail?vodId={vodId}&title={title}",
                        arguments = listOf(
                            navArgument("vodId") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                        )
                    ) { back ->
                        DetailScreen(
                            vodId = back.arguments?.getString("vodId").orEmpty(),
                            title = back.arguments?.getString("title").orEmpty(),
                            onPlay = { request ->
                                nav.navigate(
                                    "play?vodId=${Uri.encode(request.vodId)}&vodName=${Uri.encode(request.vodName)}"
                                        + "&episode=${Uri.encode(request.episode)}&line=${Uri.encode(request.line)}"
                                )
                            }
                        )
                    }

                    composable(
                        route = "play?vodId={vodId}&vodName={vodName}&episode={episode}&line={line}",
                        arguments = listOf(
                            navArgument("vodId") { type = NavType.StringType },
                            navArgument("vodName") { type = NavType.StringType },
                            navArgument("episode") { type = NavType.StringType },
                            navArgument("line") { type = NavType.StringType },
                        )
                    ) { back ->
                        PlayScreen(
                            request = PlayRequest(
                                vodId = back.arguments?.getString("vodId").orEmpty(),
                                vodName = back.arguments?.getString("vodName").orEmpty(),
                                episode = back.arguments?.getString("episode").orEmpty(),
                                line = back.arguments?.getString("line").orEmpty()
                            )
                        )
                    }
                }
            }
        }
    }
}
