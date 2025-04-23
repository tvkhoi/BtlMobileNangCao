package com.example.musicapp;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.example.musicapp.models.Song;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class SongDownloadManager {
    private static final String TAG = "SongDownloadManager";
    private static SongDownloadManager instance;
    private final Context context;
    private final File downloadListFile;

    private SongDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        // File to store downloaded songs list (JSON)
        this.downloadListFile = new File(context.getFilesDir(), "downloaded_songs.json");
    }

    // Get singleton instance
    public static synchronized SongDownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new SongDownloadManager(context);
        }
        return instance;
    }

    // Start downloading a song
    public void downloadSong(Song song, DownloadCallback callback) {
        if (song == null || song.getSongFileUrl() == null || song.getSongFileUrl().isEmpty()) {
            callback.onError("Invalid song data");
            return;
        }

        // Check if song is already downloaded
        String localPath = getLocalSongPath(song);
        if (isSongDownloaded(song)) {
            callback.onSuccess(localPath);
            return;
        }

        // Start download task
        new DownloadSongTask(song, callback).execute(song.getSongFileUrl());
    }

    // Check if song is already downloaded
    public boolean isSongDownloaded(Song song) {
        File file = new File(getLocalSongPath(song));
        return file.exists() && file.length() > 0;
    }

    // Get local file path for a song
    public String getLocalSongPath(Song song) {
        String fileName = song.getName().replaceAll("[^a-zA-Z0-9.-]", "_") + ".mp3";
        File dir;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use app-specific external storage (Scoped Storage)
            dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MusicApp");
        } else {
            // Use public external storage (requires WRITE_EXTERNAL_STORAGE permission)
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MusicApp");
        }
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, fileName).getAbsolutePath();
    }

    // Save downloaded song info to JSON
    private void saveSongToJson(Song song, String localPath) {
        try {
            JSONArray jsonArray;
            if (downloadListFile.exists()) {
                StringBuilder content = new StringBuilder();
                try (FileReader reader = new FileReader(downloadListFile)) {
                    int c;
                    while ((c = reader.read()) != -1) {
                        content.append((char) c);
                    }
                }
                jsonArray = new JSONArray(content.length() > 0 ? content.toString() : "[]");
            } else {
                jsonArray = new JSONArray();
            }

            // Check for duplicate song
            String newSongFileUrl = "file://" + localPath;
            boolean isDuplicate = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonSong = jsonArray.getJSONObject(i);
                if (jsonSong.getString("songFileUrl").equals(newSongFileUrl)) {
                    isDuplicate = true;
                    // Optionally update existing entry
                    jsonSong.put("name", song.getName());
                    jsonSong.put("artist", song.getArtist());
                    jsonSong.put("imageUrl", song.getImageUrl());
                    //jsonSong.put("liked", song.getLiked());
                    long duration = 0;
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(localPath);
                        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        if (durationStr != null) {
                            duration = Long.parseLong(durationStr);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting duration for " + localPath, e);
                    } finally {
                        retriever.release();
                    }
                    jsonSong.put("totalDuration", duration);
                    Log.d(TAG, "Updated existing song in JSON: " + song.getName());
                    break;
                }
            }

            // If not duplicate, add new song
            if (!isDuplicate) {
                JSONObject jsonSong = new JSONObject();
                jsonSong.put("name", song.getName());
                jsonSong.put("artist", song.getArtist());
                jsonSong.put("songFileUrl", newSongFileUrl);
                jsonSong.put("imageUrl", song.getImageUrl());
                //jsonSong.put("liked", song.getLiked());
                long duration = 0;
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(localPath);
                    String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (durationStr != null) {
                        duration = Long.parseLong(durationStr);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error getting duration for " + localPath, e);
                } finally {
                    retriever.release();
                }
                jsonSong.put("totalDuration", duration);
                jsonArray.put(jsonSong);
                Log.d(TAG, "Saved new song to JSON: " + song.getName());
            }

            // Write back to file
            try (FileWriter writer = new FileWriter(downloadListFile)) {
                writer.write(jsonArray.toString(2)); // Pretty print with indent
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving song to JSON: " + e.getMessage(), e);
        }
    }

    // Get list of downloaded songs from JSON
    public ArrayList<Song> getDownloadedSongs() {
        ArrayList<Song> downloadedSongs = new ArrayList<>();
        try {
            if (!downloadListFile.exists()) {
                return downloadedSongs;
            }

            StringBuilder content = new StringBuilder();
            try (FileReader reader = new FileReader(downloadListFile)) {
                int c;
                while ((c = reader.read()) != -1) {
                    content.append((char) c);
                }
            }

            JSONArray jsonArray = new JSONArray(content.length() > 0 ? content.toString() : "[]");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonSong = jsonArray.getJSONObject(i);
                Song song = new Song();
                song.setName(jsonSong.getString("name"));
                song.setArtist(jsonSong.getString("artist"));
                song.setSongFileUrl(jsonSong.getString("songFileUrl"));
                song.setImageUrl(jsonSong.optString("imageUrl", ""));
                song.setTotalDuration(jsonSong.getInt("totalDuration"));
                //song.setLiked(jsonSong.getInt("liked"));
                // Verify file exists
                String localPath = song.getSongFileUrl().startsWith("file://") ?
                        song.getSongFileUrl().substring(7) : song.getSongFileUrl();
                if (new File(localPath).exists()) {
                    downloadedSongs.add(song);
                } else {
                    Log.w(TAG, "File not found, skipping: " + localPath);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading downloaded songs: " + e.getMessage(), e);
        }
        return downloadedSongs;
    }

    // Get the number of downloaded songs
    public int getDownloadedSongsCount() {
        int count = 0;
        try {
            if (!downloadListFile.exists()) {
                return count;
            }

            StringBuilder content = new StringBuilder();
            try (FileReader reader = new FileReader(downloadListFile)) {
                int c;
                while ((c = reader.read()) != -1) {
                    content.append((char) c);
                }
            }

            JSONArray jsonArray = new JSONArray(content.length() > 0 ? content.toString() : "[]");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonSong = jsonArray.getJSONObject(i);
                String songFileUrl = jsonSong.getString("songFileUrl");
                String localPath = songFileUrl.startsWith("file://") ?
                        songFileUrl.substring(7) : songFileUrl;
                if (new File(localPath).exists()) {
                    count++;
                } else {
                    Log.w(TAG, "File not found, skipping count for: " + localPath);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error counting downloaded songs: " + e.getMessage(), e);
        }
        return count;
    }

    // Update song with local path after download
    private void updateSongWithLocalPath(Song song, String localPath) {
        // Save to JSON instead of updating StorageSong
        saveSongToJson(song, localPath);
    }

    // AsyncTask to download song
    private class DownloadSongTask extends AsyncTask<String, Integer, String> {
        private final Song song;
        private final DownloadCallback callback;
        private String errorMessage;

        DownloadSongTask(Song song, DownloadCallback callback) {
            this.song = song;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... urls) {
            String urlString = urls[0];
            HttpURLConnection connection = null;
            InputStream input = null;
            FileOutputStream output = null;

            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    errorMessage = "Server returned HTTP " + connection.getResponseCode();
                    return null;
                }

                String localPath = getLocalSongPath(song);
                File outputFile = new File(localPath);
                File parentDir = outputFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }

                input = new BufferedInputStream(connection.getInputStream());
                output = new FileOutputStream(outputFile);

                byte[] buffer = new byte[1024];
                int bytesRead;
                long totalBytesRead = 0;
                long fileSize = connection.getContentLength();
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    if (fileSize > 0) {
                        int progress = (int) ((totalBytesRead * 100) / fileSize);
                        publishProgress(progress);
                    }
                }

                return localPath;
            } catch (IOException e) {
                errorMessage = "Download failed: " + e.getMessage();
                Log.e(TAG, errorMessage, e);
                return null;
            } finally {
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (connection != null) connection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            callback.onProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String localPath) {
            if (localPath != null) {
                updateSongWithLocalPath(song, localPath);
                callback.onSuccess(localPath);
                Toast.makeText(context, "Song downloaded: " + song.getName(), Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Song downloaded to: " + localPath);
            } else {
                callback.onError(errorMessage);
                Toast.makeText(context, "Download failed: " + errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "Download failed: " + errorMessage);
            }
        }
    }

    // Callback interface for download events
    public interface DownloadCallback {
        void onSuccess(String localPath);
        void onError(String errorMessage);
        void onProgress(int progress);
    }
}