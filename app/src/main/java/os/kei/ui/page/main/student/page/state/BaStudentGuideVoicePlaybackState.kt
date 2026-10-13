package os.kei.ui.page.main.student.page.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

@Immutable
internal data class BaStudentGuideVoicePlaybackState(
    val playingVoiceUrl: String,
    val isVoicePlaying: Boolean,
)

/** Only playback identity and controls invalidate the page; progress is read by the progress ring. */
@Composable
internal fun rememberBaStudentGuideVoicePlaybackState(
    voiceUiState: State<BaStudentGuideVoiceUiState>,
): State<BaStudentGuideVoicePlaybackState> = remember(voiceUiState) {
    derivedStateOf {
        val voice = voiceUiState.value
        BaStudentGuideVoicePlaybackState(voice.playingVoiceUrl, voice.isVoicePlaying)
    }
}
