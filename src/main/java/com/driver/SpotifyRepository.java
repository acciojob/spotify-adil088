package com.driver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class SpotifyRepository {
    public HashMap<Artist, List<Album>> artistAlbumMap;
    public HashMap<Album, List<Song>> albumSongMap;
    public HashMap<Playlist, List<Song>> playlistSongMap;
    public HashMap<Playlist, List<User>> playlistListenerMap;
    public HashMap<User, Playlist> creatorPlaylistMap;
    public HashMap<User, List<Playlist>> userPlaylistMap;
    public HashMap<Song, List<User>> songLikeMap;

    public List<User> users;
    public List<Song> songs;
    public List<Playlist> playlists;
    public List<Album> albums;
    public List<Artist> artists;

    public SpotifyRepository() {
        // To avoid hitting apis multiple times, initialize all the hashmaps here with
        // some dummy data
        artistAlbumMap = new HashMap<>();
        albumSongMap = new HashMap<>();
        playlistSongMap = new HashMap<>();
        playlistListenerMap = new HashMap<>();
        creatorPlaylistMap = new HashMap<>();
        userPlaylistMap = new HashMap<>();
        songLikeMap = new HashMap<>();

        users = new ArrayList<>();
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
    }

    public User createUser(String name, String mobile) {
        User user = new User();
        user.setName(name);
        user.setMobile(mobile);
        users.add(user);
        return user;
    }

    public Artist createArtist(String name) {
        Artist artist = new Artist();
        artist.setName(name);
        artists.add(artist);
        return artist;
    }

    public Album createAlbum(String title, String artistName) {
        Artist existingArtist = null;
        for (Artist artist : artists) {
            if (artist.getName().equalsIgnoreCase(artistName)) {
                existingArtist = artist;
                break;
            }
        }

        if (existingArtist == null) {
            existingArtist = createArtist(artistName);
        }

        Album album = new Album();
        album.setTitle(title);
        albums.add(album);

        artistAlbumMap.computeIfAbsent(existingArtist, k -> new ArrayList<>()).add(album);

        return album;
    }

    public Song createSong(String title, String albumName, int length) throws Exception {

        for (Album album : albums) {
            if (album.getTitle().equalsIgnoreCase(albumName)) {
                Song song = new Song();
                song.setTitle(title);
                song.setLength(length);
                songs.add(song);
                albumSongMap.computeIfAbsent(album, k -> new ArrayList<>()).add(song);
                return song;
            }
        }

        throw new Exception();
    }

    public Playlist createPlaylistOnLength(String mobile, String title, int length) throws Exception {

        // Ensure that users and songs lists are not null
        if (users == null || songs == null) {
            throw new Exception("Users or Songs list is not initialized.");
        }

        User currentUser = null;
        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                currentUser = user; // Found the user with the given mobile number
                break;
            }
        }

        if (currentUser == null) {
            throw new Exception("User not found for mobile: " + mobile);
        }

        // Create a new playlist
        Playlist playlist = new Playlist();
        playlist.setTitle(title);

        // Filter songs based on the given length
        List<Song> filteredSongs = new ArrayList<>();
        for (Song song : songs) {
            if (song.getLength() == length) {
                filteredSongs.add(song);
            }
        }

        if (filteredSongs.isEmpty()) {
            throw new Exception("No songs found with the given length: " + length);
        }

        // Add the playlist to the list of playlists
        playlists.add(playlist);

        // Map the playlist to the filtered songs
        playlistSongMap.computeIfAbsent(playlist, k -> new ArrayList<>()).addAll(filteredSongs);

        return playlist;
    }

    public Playlist createPlaylistOnName(String mobile, String title,
            List<String> songTitles) throws Exception {

        // Ensure that users and songs lists are not null
        if (users == null || songs == null) {
            throw new Exception("Users or Songs list is not initialized.");
        }

        User currentUser = null;
        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                currentUser = user; // Found the user with the given mobile number
                break;
            }
        }

        if (currentUser == null) {
            throw new Exception("User not found for mobile: " + mobile);
        }

        // Create a new playlist
        Playlist playlist = new Playlist();
        playlist.setTitle(title);

        List<Song> selectedSongs = new ArrayList<>();
        for (String songTitle : songTitles) {
            boolean songFound = false;
            for (Song song : songs) {
                if (song.getTitle().equalsIgnoreCase(songTitle)) {
                    selectedSongs.add(song);
                    songFound = true;
                    break;
                }
            }
            if (!songFound) {
                throw new Exception("Song with title '" + songTitle + "' not found.");
            }
        }

        // Add the playlist to the playlists list
        playlists.add(playlist);

        // Map the playlist to the selected songs
        playlistSongMap.computeIfAbsent(playlist, k -> new ArrayList<>()).addAll(selectedSongs);

        // Map the playlist to the user (creator) and the listeners
        playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(currentUser);
        creatorPlaylistMap.put(currentUser, playlist);
        userPlaylistMap.computeIfAbsent(currentUser, k -> new ArrayList<>()).add(playlist);

        return playlist;
    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        Playlist foundPlaylist = null;
        User currentUser = null;

        // Ensure that users and playlists are not null
        if (users == null || playlists == null) {
            throw new NullPointerException("Users or playlists are not initialized!");
        }

        // Find the current user
        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                currentUser = user;
                break; // Stop once the user is found
            }
        }

        if (currentUser == null) {
            throw new Exception("User with mobile " + mobile + " not found.");
        }

        // Find the playlist
        for (Playlist playlist : playlists) {
            if (playlist.getTitle().equalsIgnoreCase(playlistTitle)) {
                foundPlaylist = playlist;
                break; // Stop once the playlist is found
            }
        }

        if (foundPlaylist == null) {
            throw new Exception("Playlist with title " + playlistTitle + " not found.");
        }

        // Ensure that playlistListenerMap is initialized
        if (playlistListenerMap == null) {
            playlistListenerMap = new HashMap<>();
        }

        // Check if the user is the creator or a listener
        boolean isListener = false;
        boolean isCreator = false;

        // Check if the user is a listener
        List<User> listeners = playlistListenerMap.get(foundPlaylist);
        if (listeners != null) {
            isListener = listeners.contains(currentUser);
        }

        // Check if the user is the creator
        Playlist creatorPlaylist = creatorPlaylistMap.get(currentUser);
        if (creatorPlaylist != null && creatorPlaylist.equals(foundPlaylist)) {
            isCreator = true;
        }

        // If the user is neither a listener nor the creator, add them as a listener
        if (!isListener && !isCreator) {
            playlistListenerMap.computeIfAbsent(foundPlaylist, k -> new ArrayList<>()).add(currentUser);
        }

        return foundPlaylist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {

        Song foundSong = null;

        // Ensure songs list is not null
        if (songs == null) {
            throw new NullPointerException("Songs list is not initialized!");
        }

        // Find the song by title
        for (Song song : songs) {
            if (song.getTitle().equalsIgnoreCase(songTitle)) {
                foundSong = song;
                break; // Stop once the song is found
            }
        }

        if (foundSong == null) {
            throw new Exception("Song with title " + songTitle + " not found!");
        }

        User foundUser = null;

        // Ensure users list is not null
        if (users == null) {
            throw new NullPointerException("Users list is not initialized!");
        }

        // Find the user by mobile number
        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                foundUser = user;
                break; // Stop once the user is found
            }
        }

        if (foundUser == null) {
            throw new Exception("User with mobile " + mobile + " not found!");
        }

        // Check if user already liked the song
        List<User> songLikers = songLikeMap.computeIfAbsent(foundSong, k -> new ArrayList<>());
        if (songLikers.contains(foundUser)) {
            throw new Exception("User has already liked this song!");
        }

        // Add user to the likers list and increment the song's like count
        songLikers.add(foundUser);
        foundSong.setLikes(foundSong.getLikes() + 1);

        // Ensure albumSongMap is initialized
        if (albumSongMap == null) {
            albumSongMap = new HashMap<>();
        }

        Album foundAlbum = null;

        // Find the album containing the song
        for (Map.Entry<Album, List<Song>> entry : albumSongMap.entrySet()) {
            List<Song> albumSongs = entry.getValue();
            if (albumSongs.contains(foundSong)) {
                foundAlbum = entry.getKey();
                break; // Stop once the album is found
            }
        }

        if (foundAlbum != null) {
            // Ensure artistAlbumMap is initialized
            if (artistAlbumMap == null) {
                artistAlbumMap = new HashMap<>();
            }

            // Update the artist's like count if the song's album is associated with an
            // artist
            for (Map.Entry<Artist, List<Album>> entry : artistAlbumMap.entrySet()) {
                List<Album> albums = entry.getValue();
                if (albums.contains(foundAlbum)) {
                    entry.getKey().setLikes(entry.getKey().getLikes() + 1);
                    break; // Stop once the artist is found
                }
            }
        }

        return foundSong;
    }

    public String mostPopularArtist() {

        String mostPopularArtist = null;
        int maxLikes = 0;

        if (artists.isEmpty()) {
            return "Artist not found!!";
        }

        for (Artist artist : artists) {
            if (artist.getLikes() > maxLikes) {
                maxLikes = artist.getLikes();
                mostPopularArtist = artist.getName();
            }
        }

        return (mostPopularArtist != null) ? mostPopularArtist : "No artist with likes found!";

    }

    public String mostPopularSong() {
        String mostPopularSong = null;
        int maxLikes = 0;

        if (songs.isEmpty()) {
            return "Song not found!!";
        }

        for (Song song : songs) {
            if (song.getLikes() > maxLikes) {
                maxLikes = song.getLikes();
                mostPopularSong = song.getTitle();
            }
        }

        return (mostPopularSong != null) ? mostPopularSong : "No song with likes found!";
    }
}
