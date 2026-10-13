package tr.canugur.mywayfiles

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import tr.canugur.mywayfiles.ui.home.HomeScreen
import tr.canugur.mywayfiles.ui.theme.MyWayFilesTheme

@Composable
@Preview
fun App() {
    MyWayFilesTheme {
        HomeScreen()
    }
}
