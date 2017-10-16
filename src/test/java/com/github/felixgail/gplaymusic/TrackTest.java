package com.github.felixgail.gplaymusic;

import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.model.enums.StreamQuality;
import com.github.felixgail.gplaymusic.model.shema.Playlist;
import com.github.felixgail.gplaymusic.model.shema.PlaylistEntry;
import com.github.felixgail.gplaymusic.model.shema.Track;
import com.github.felixgail.gplaymusic.util.TestUtil;
import org.apache.tika.Tika;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import svarzee.gps.gpsoauth.Gpsoauth;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import static com.github.felixgail.gplaymusic.util.TestUtil.assertTracks;
import static com.github.felixgail.gplaymusic.util.TestUtil.assume;
import static org.junit.Assert.assertNotNull;

public class TrackTest extends TestWithLogin {

  @BeforeClass
  public static void before() throws IOException, Gpsoauth.TokenRequestFailed {
    loginToService(TestUtil.USERNAME, TestUtil.PASSWORD, TestUtil.ANDROID_ID, TestUtil.TOKEN);
  }

  @Test
  public void getTrackUrl() throws IOException {
    Track track = GPlayMusic.getApiInstance().searchTracks("Sound", 10).get(0);
    assertTracks(track);
    String url = track.getStreamURL(StreamQuality.HIGH);
    assertNotNull(url);
    Assert.assertTrue(url.startsWith("http"));
  }

  @Test
  public void incrementPlaycount() throws IOException {
    Track track = GPlayMusic.getApiInstance().searchTracks("Sound", 10).get(0);
    assume(track);
    assume(track.getPlayCount().isPresent());
    int playcount = track.getPlayCount().getAsInt();
    int inc = 2;
    track.incrementPlaycount(inc);
    Assert.assertEquals("Playcount was not increased locally.",
        playcount + inc, track.getPlayCount().getAsInt());
    Track trackNew = GPlayMusic.getApiInstance().searchTracks("Sound", 10).get(0);
    Assume.assumeTrue("Newly fetched track should equal original track",
        track.getID().equals(trackNew.getID()));
    Assert.assertEquals("Playcount was not increased at remote location.",
        playcount + inc, trackNew.getPlayCount().getAsInt());
  }

  @Test
  public void testDownloadSearch() throws IOException {
    Track track = GPlayMusic.getApiInstance().searchTracks("Sound", 10).get(0);
    assume(track);
    testDownload("searchedTrack.mp3", track);
  }

  @Test
  public void testPlaylistDownload() throws IOException {
    //PlaylistID with key test.track.playlist should be a playlist conatining both store and library tracks.
    //To find it use printAllPlaylists();
    Playlist playlist = new Playlist(TestUtil.get("test.track.playlist").get());
    Track track;
    for (PlaylistEntry entry : playlist.getContents(-1)) {
      track = entry.getTrack();
      System.out.printf("Preparing to download '%s'\n", track.getTitle());
      testDownload(track.getTitle(), track);
      System.out.println("\t - Completed");
    }
  }

  private void printAllPlaylists() throws IOException {
    GPlayMusic.getApiInstance().listPlaylists().forEach(p -> System.out.printf("%s: %s\n", p.getName(), p.getId()));
  }

  private void testDownload(String fileName, Track track) throws IOException {
    Path path = FileSystems.getDefault().getPath(System.getProperty("java.io.tmpdir"), fileName);
    track.download(StreamQuality.LOW, path);
    File file = path.toFile();
    Assert.assertTrue("File does not exist", file.exists());
    Assert.assertEquals("Is not an audio file", new Tika().detect(file), "audio/mpeg");
  }
}
