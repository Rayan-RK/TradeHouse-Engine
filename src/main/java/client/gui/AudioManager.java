package client.gui;

import java.io.BufferedInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 * Lightweight sound manager for UI feedback cues loaded from bundled WAV files.
 */
public final class AudioManager {
    private static final String SUCCESS_CLIP = "/client/gui/sounds/success.wav";
    private static final String ERROR_CLIP = "/client/gui/sounds/error.wav";

    private static volatile boolean enabled = true;

    private AudioManager() {
    }

    public static void setEnabled(boolean soundEnabled) {
        enabled = soundEnabled;
    }

    public static void playSuccess() {
        if (!enabled) {
            return;
        }
        playClip(SUCCESS_CLIP, true);
    }

    public static void playError() {
        if (!enabled) {
            return;
        }
        playClip(ERROR_CLIP, false);
    }

    private static void playClip(String resourcePath, boolean successFallback) {
        Thread thread = new Thread(() -> {
            try (InputStream rawStream = AudioManager.class.getResourceAsStream(resourcePath)) {
                if (rawStream == null) {
                    playFallback(successFallback);
                    return;
                }

                try (BufferedInputStream bufferedStream = new BufferedInputStream(rawStream);
                        AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedStream)) {
                    Clip clip = AudioSystem.getClip();
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) {
                            clip.close();
                        }
                    });
                    clip.open(audioStream);
                    clip.start();
                }
            } catch (Exception ignored) {
                playFallback(successFallback);
            }
        }, "tradehouse-audio-manager");
        thread.setDaemon(true);
        thread.start();
    }

    private static void playFallback(boolean successFallback) {
        if (successFallback) {
            AudioCue.success();
        } else {
            AudioCue.error();
        }
    }
}