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

        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                Playlist playlist = new Playlist();
                playlist.setTitle(title);

                List<Song> filteredSongs = new ArrayList<>();
                for (Song song : songs) {
                    if (song.getLength() == length) {
                        filteredSongs.add(song);
                    }
                }
                playlists.add(playlist);
                playlistSongMap.computeIfAbsent(playlist, k -> new ArrayList<>()).addAll(filteredSongs);
                return playlist;
            }
        }

        throw new Exception();

    }

    public Playlist createPlaylistOnName(String mobile, String title,
            List<String> songTitles) throws Exception {

        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
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
                        throw new Exception();
                    }
                }
                playlists.add(playlist);
                playlistSongMap.computeIfAbsent(playlist, k -> new ArrayList<>()).addAll(selectedSongs);
                playlistListenerMap.computeIfAbsent(playlist, k -> new ArrayList<>()).add(user);
                creatorPlaylistMap.put(user, playlist);
                userPlaylistMap.computeIfAbsent(user, k -> new ArrayList<>()).add(playlist);

                return playlist;
            }
        }
        throw new Exception();

    }

    public Playlist findPlaylist(String mobile, String playlistTitle) throws Exception {
        Playlist foundPlaylist = null;
        User currentUser = null;
        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                currentUser = user;
                for (Playlist playlist : playlists) {
                    if (playlist.getTitle().equalsIgnoreCase(playlistTitle)) {
                        foundPlaylist = playlist;
                        break;
                    }
                }

            }
        }

        if (foundPlaylist != null) {
            boolean isListener = false;
            boolean isCreator = false;
            for (User user : users) {
                if (user.getMobile().equalsIgnoreCase(mobile)) {
                    currentUser = user;
                    break; // Stop once the user is found
                }
            }

            if (currentUser == null) {
                throw new Exception();
            }

            for (Playlist playlist : playlists) {
                if (playlist.getTitle().equalsIgnoreCase(playlistTitle)) {
                    foundPlaylist = playlist;
                    break; // Stop once the playlist is found
                }
            }

            if (foundPlaylist != null) {
                // throw new Exception("Playlist not found");

                Playlist creatorPlaylist = creatorPlaylistMap.get(currentUser);
                isCreator = (creatorPlaylist != null && creatorPlaylist.equals(foundPlaylist));

                List<User> listeners = playlistListenerMap.get(foundPlaylist);
                isListener = (listeners != null && listeners.contains(currentUser));

                if (!isListener && !isCreator) {
                    playlistListenerMap.computeIfAbsent(foundPlaylist, k -> new ArrayList<>()).add(currentUser);
                }
            }

        }
        return foundPlaylist;
    }

    public Song likeSong(String mobile, String songTitle) throws Exception {

        Song foundSong = null;
        for (Song song : songs) {
            if (song.getTitle().equalsIgnoreCase(songTitle)) {
                foundSong = song;
                break;
            }
        }
        if (foundSong == null) {
            throw new Exception();
        }

        User foundUser = null;
        for (User user : users) {
            if (user.getMobile().equalsIgnoreCase(mobile)) {
                foundUser = user;
                break;
            }
        }
        if (foundUser == null) {
            throw new Exception();
        }

        List<User> songLikers = songLikeMap.computeIfAbsent(foundSong, k -> new ArrayList<>());
        if (songLikers.contains(foundUser)) {
            throw new Exception();
        }
        songLikers.add(foundUser);
        foundSong.setLikes(foundSong.getLikes() + 1);

        Album foundAlbum = null;
        for (Map.Entry<Album, List<Song>> entry : albumSongMap.entrySet()) {
            Album album = entry.getKey();
            List<Song> albumSongs = entry.getValue();
            if (albumSongs.contains(foundSong)) {
                foundAlbum = album;
                break;
            }
        }

        if (foundAlbum != null) {
            for (Map.Entry<Artist, List<Album>> entry : artistAlbumMap.entrySet()) {
                Artist artist = entry.getKey();
                List<Album> albums = entry.getValue();
                if (albums.contains(foundAlbum)) {
                    artist.setLikes(artist.getLikes() + 1);
                    break;
                }
            }
        }

        return foundSong;

    }

    public String mostPopularArtist() {

        String mostPopularArtist = "";
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

        if (mostPopularArtist.equalsIgnoreCase("")) {
            return "No artist with likes found!";
        }

        return mostPopularArtist;

    }

    public String mostPopularSong() {
        String mostPopularSong = "";
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

        if (mostPopularSong.equalsIgnoreCase("")) {
            return "No song with likes found!";
        }

        return mostPopularSong;
    }
}
